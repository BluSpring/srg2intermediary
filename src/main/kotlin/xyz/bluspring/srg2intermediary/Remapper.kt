package xyz.bluspring.srg2intermediary

import net.minecraftforge.srgutils.IMappingBuilder
import net.minecraftforge.srgutils.IMappingFile
import java.io.File

class Remapper(downloader: MappingDownloader, private val version: String) {
    val srgMappings = IMappingFile.load(downloader.srgMappingsFile).reverse() // srg -> official (result of being reversed)

    val mojangMappings = IMappingFile.load(downloader.mojangMappingsFile).reverse() // official -> moj (result of being reversed)
    val intermediaryMappings = IMappingFile.load(downloader.intermediaryFile) // official -> intermediary

    fun remap() {
        val startTime = System.currentTimeMillis()
        println("Creating mappings with MojMap class names and SRG method/field names to Intermediary...")

        // the args for each of these is (from -> to)
        val mappingBuilder = IMappingBuilder.create("srg", "intermediary")
        srgMappings.classes.forEach { srgClass ->
            val mojangClass = mojangMappings.getClass(srgClass.mapped)
            val intermediaryClass = intermediaryMappings.getClass(srgClass.mapped)

            if (intermediaryClass == null) {
                println("Failed to map class: SRG:${srgClass.original} OFFICIAL:${srgClass.mapped} MOJ:${mojangClass.mapped}")

                return@forEach
            }

            val mappedClass = mappingBuilder.addClass(mojangClass.mapped, intermediaryClass.mapped)

            srgClass.fields.forEach fieldMapper@{ field ->
                val intermediaryField = intermediaryClass.getField(field.mapped) ?: return@fieldMapper

                mappedClass.field(field.original, intermediaryField.mapped)
                    .descriptor(intermediaryField.mappedDescriptor)
            }

            srgClass.methods.forEach methodMapper@{ method ->
                val intermediaryMethod = intermediaryClass.getMethod(method.mapped, method.mappedDescriptor) ?: return@methodMapper

                mappedClass.method(intermediaryMethod.mappedDescriptor, method.original, intermediaryMethod.mapped)
                    .apply {
                        intermediaryMethod.parameters.forEach { param ->
                            this.parameter(param.index, param.mapped)
                        }
                    }
            }
        }


        val remappedFile = File(System.getProperty("user.dir"), "remapped_$version.tiny")
        mappingBuilder.build().write(remappedFile.toPath(), IMappingFile.Format.TINY)

        println("Finished creating a map for SRG to Intermediary! The file has been created at ${remappedFile.absolutePath}. (took ${System.currentTimeMillis() - startTime}ms)")
    }
}