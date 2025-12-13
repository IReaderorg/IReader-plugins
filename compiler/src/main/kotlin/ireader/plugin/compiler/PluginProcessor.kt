package ireader.plugin.compiler

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import ireader.plugin.annotations.IReaderPlugin

/**
 * KSP processor for IReader plugins.
 * Generates PluginFactory class for runtime plugin discovery.
 */
class PluginProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private var processed = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (processed) return emptyList()

        val pluginClasses = resolver
            .getSymbolsWithAnnotation(IReaderPlugin::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }
            .toList()

        if (pluginClasses.isEmpty()) {
            return emptyList()
        }

        processed = true

        // Generate plugin factory
        generatePluginFactory(pluginClasses)

        logger.info("Processed ${pluginClasses.size} plugin classes")

        return emptyList()
    }

    private fun generatePluginFactory(pluginClasses: List<KSClassDeclaration>) {
        val packageName = "ireader.plugin.generated"
        val className = "PluginFactory"

        val fileSpec = FileSpec.builder(packageName, className)
            .addType(
                TypeSpec.objectBuilder(className)
                    .addFunction(
                        FunSpec.builder("createPlugin")
                            .addParameter("className", String::class)
                            .returns(Any::class.asTypeName().copy(nullable = true))
                            .addCode(buildCodeBlock {
                                beginControlFlow("return when (className)")
                                pluginClasses.forEach { pluginClass ->
                                    val qualifiedName = pluginClass.qualifiedName?.asString()
                                    addStatement(
                                        "%S -> %T()",
                                        qualifiedName,
                                        pluginClass.toClassName()
                                    )
                                }
                                addStatement("else -> null")
                                endControlFlow()
                            })
                            .build()
                    )
                    .addFunction(
                        FunSpec.builder("getAllPluginClasses")
                            .returns(
                                List::class.asTypeName().parameterizedBy(String::class.asTypeName())
                            )
                            .addCode(buildCodeBlock {
                                add("return listOf(\n")
                                indent()
                                pluginClasses.forEachIndexed { index, pluginClass ->
                                    val qualifiedName = pluginClass.qualifiedName?.asString()
                                    if (index == pluginClasses.size - 1) {
                                        addStatement("%S", qualifiedName)
                                    } else {
                                        addStatement("%S,", qualifiedName)
                                    }
                                }
                                unindent()
                                add(")\n")
                            })
                            .build()
                    )
                    .addFunction(
                        FunSpec.builder("getPluginInfo")
                            .addParameter("className", String::class)
                            .returns(ClassName("ireader.plugin.compiler", "PluginMetadata").copy(nullable = true))
                            .addCode(buildCodeBlock {
                                beginControlFlow("return when (className)")
                                pluginClasses.forEach { pluginClass ->
                                    val qualifiedName = pluginClass.qualifiedName?.asString()
                                    val info = extractPluginInfo(pluginClass)
                                    addStatement(
                                        "%S -> PluginMetadata(%S, %S, %S, %L)",
                                        qualifiedName,
                                        info.id,
                                        info.name,
                                        info.version,
                                        info.versionCode
                                    )
                                }
                                addStatement("else -> null")
                                endControlFlow()
                            })
                            .build()
                    )
                    .build()
            )
            .addType(
                TypeSpec.classBuilder("PluginMetadata")
                    .addModifiers(KModifier.DATA)
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameter("id", String::class)
                            .addParameter("name", String::class)
                            .addParameter("version", String::class)
                            .addParameter("versionCode", Int::class)
                            .build()
                    )
                    .addProperty(PropertySpec.builder("id", String::class).initializer("id").build())
                    .addProperty(PropertySpec.builder("name", String::class).initializer("name").build())
                    .addProperty(PropertySpec.builder("version", String::class).initializer("version").build())
                    .addProperty(PropertySpec.builder("versionCode", Int::class).initializer("versionCode").build())
                    .build()
            )
            .build()

        fileSpec.writeTo(codeGenerator, Dependencies(false, *pluginClasses.map { it.containingFile!! }.toTypedArray()))
    }

    private fun extractPluginInfo(pluginClass: KSClassDeclaration): PluginInfoData {
        val pluginInfo = pluginClass.annotations
            .find { it.shortName.asString() == "PluginInfo" }

        return PluginInfoData(
            id = pluginInfo?.arguments?.find { it.name?.asString() == "id" }?.value as? String ?: "",
            name = pluginInfo?.arguments?.find { it.name?.asString() == "name" }?.value as? String ?: pluginClass.simpleName.asString(),
            version = pluginInfo?.arguments?.find { it.name?.asString() == "version" }?.value as? String ?: "1.0.0",
            versionCode = pluginInfo?.arguments?.find { it.name?.asString() == "versionCode" }?.value as? Int ?: 1
        )
    }

    private data class PluginInfoData(
        val id: String,
        val name: String,
        val version: String,
        val versionCode: Int
    )
}

class PluginProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return PluginProcessor(environment.codeGenerator, environment.logger)
    }
}
