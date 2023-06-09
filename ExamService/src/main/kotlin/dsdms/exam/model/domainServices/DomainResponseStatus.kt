/*******************************************************************************
 * MIT License
 *
 * Copyright 2023 DSDMS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE
 ******************************************************************************/

package dsdms.exam.model.domainServices

import dsdms.exam.handlers.domainConversionTable

/**
 * Types of domain responses.
 * @see domainConversionTable
 */
enum class DomainResponseStatus {
    /**
     * Otherwise...
     */
    OK,

    /**
     * Dossier id already has one valid theoretical exam pass.
     */
    EXAM_PASS_ALREADY_AVAILABLE,

    /**
     * Delete result was not acknowledged.
     */
    DELETE_ERROR,

    /**
     * Not found theoretical exam passes with given dossier id.
     */
    ID_NOT_FOUND,

    /**
     * Wanted exam date to insert, already found.
     */
    DATE_ALREADY_IN,

    /**
     * Exam day insert was not acknowledged.
     */
    INSERT_ERROR,

    /**
     * Requesting next exam appeals, but no one found.
     */
    NO_EXAM_APPEALS,

    /**
     * Attempting to insert a dossier in a not existent exam appeal.
     */
    APPEAL_NOT_FOUND,

    /**
     * Attempting to register a new Dossier in a full theoretical exam appeal.
     */
    PLACES_FINISHED,

    /**
     * There was an error in updating exam appeal to insert a dossier id into it.
     */
    UPDATE_ERROR,

    /**
     * Dossier id already in list for a theoretical exam appeal.
     */
    DOSSIER_ALREADY_PUT,

    /**
     * There are already a valid provisional license associated with dossier.
     */
    PROVISIONAL_LICENSE_ALREADY_EXISTS,

    /**
     * Update exam status in dossier context finish with an error.
     */
    EXAM_STATUS_UPDATE_ERROR,

    /**
     * Indicate that provisional license is not valid on requested date.
     */
    PROVISIONAL_LICENSE_NOT_VALID,

    /**
     * Indicate that operation can not be done because dossier is not valid.
     */
    DOSSIER_NOT_VALID,

    /**
     * Indicate that operation can not be done because dossier do not exist.
     */
    DOSSIER_NOT_EXIST,
}
