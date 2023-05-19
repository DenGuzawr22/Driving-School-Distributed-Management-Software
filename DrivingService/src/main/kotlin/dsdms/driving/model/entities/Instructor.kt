package dsdms.driving.model.entities

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Represents info about instructors
 * @param name
 * @param surname
 * @param fiscal_code
 * @param _id: auto generated by Mongo, can be used to access unequivocal each element
 */
@Serializable
data class Instructor(
    val name: String,
    val surname: String,
    val fiscal_code: String,
    @Contextual val _id: String? = null
)