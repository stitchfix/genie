package com.netflix.genie.server.services;

import java.util.Set;

import com.netflix.genie.common.exceptions.GenieException;
import com.netflix.genie.common.model.FileAttachment;
import org.springframework.validation.annotation.Validated;

/**
 * Interface for a service that can temporarily persist file attachments if we cannot
 * start a job immediately.
 *
 * Created by derekbennett on 1/11/17.
 */
@Validated
public interface FileAttachmentStorage {

    /**
     * Retrieve the attachments for the given job Id, returning an empty set if none stored.
     *
     * This is a non-destructive get that leaves the stored attachments in place.
     *
     * @param jobId The Job Id of the attachments to fetch
     * @return The set of file attachments from the job, which could be empty
     * @throws GenieException if there is any error
     */
    Set<FileAttachment> getAttachments(
            final String jobId
    ) throws GenieException;


    /**
     * Retrieve the attachments for the given job Id and remove them from storage,
     * returning an empty set if none were stored.
     *
     * This is a combination of a get and delete, so that storage can be cleaned up when
     * a job is run.
     *
     * @param jobId The Job Id of the attachments to fetch
     * @return The set of file attachments from the job, which could be empty
     * @throws GenieException if there is any error
     */
    Set<FileAttachment> unstoreAttachments(
            final String jobId
    ) throws GenieException;


    /**
     * Store the attachments for the given job Id.
     *
     * @param jobId The Job Id of the attachments to store
     * @param attachments The set of file attachments from the job, which can be empty
     * @throws GenieException if there is any error
     */
    void storeAttachments(
            final String jobId,
            final Set<FileAttachment> attachments
    ) throws GenieException;
}
