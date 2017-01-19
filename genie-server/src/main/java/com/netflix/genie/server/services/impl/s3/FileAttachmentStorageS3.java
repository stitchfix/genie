package com.netflix.genie.server.services.impl.s3;

import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.MultiObjectDeleteException;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.google.common.io.ByteStreams;
import com.netflix.config.ConfigurationManager;
import com.netflix.genie.common.exceptions.GenieException;
import com.netflix.genie.common.exceptions.GenieServerException;
import com.netflix.genie.common.model.FileAttachment;
import com.netflix.genie.server.services.FileAttachmentStorage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.apache.commons.configuration.AbstractConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;


/**
 * Implementation of file attachment storage interface that uses a folder on S3 to keep
 * the attachments until we want to actually run the job
 *
 * Created by derekbennett on 1/11/17.
 */
public class FileAttachmentStorageS3 implements FileAttachmentStorage {

    private static final Logger LOG = LoggerFactory.getLogger(FileAttachmentStorage.class);

    // instance of the netflix configuration object
    private static final AbstractConfiguration CONF;

    // initialize static variables
    static {
        CONF = ConfigurationManager.getConfigInstance();
    }

    private AmazonS3Client amazonS3Client;


    /**
     * Create an instance of the FileAttachmentStorageS3 object.
     *
     * Initializes the S3 client so we can store job files on S3 temporarily.
     */
    public FileAttachmentStorageS3() {
        final DefaultAWSCredentialsProviderChain defaultChain = new DefaultAWSCredentialsProviderChain();
        this.amazonS3Client = new AmazonS3Client(defaultChain);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Set<FileAttachment> getAttachments(
            final String jobId) throws GenieException {
        final String jobBaseURI = this.getS3JobDir() + jobId + "/";
        final List<S3ObjectSummary> objectList = this.getObjectSummaries(jobBaseURI);
        final HashSet<FileAttachment> attachments = new HashSet<>();

        try {
            for (S3ObjectSummary objInfo : objectList) {
                if (objInfo.getKey().endsWith("/")) {
                    continue;
                }

                LOG.info(String.format("Getting : %s - %s", objInfo.getBucketName(), objInfo.getKey()));
                final S3Object s3Object = this.amazonS3Client.getObject(
                        new GetObjectRequest(objInfo.getBucketName(), objInfo.getKey()));

                // Store in file attachments object & add to the set
                final FileAttachment currObj = new FileAttachment();
                currObj.setName(this.filenameFromKey(s3Object.getKey()));
                currObj.setData(this.getObjectData(s3Object));
                attachments.add(currObj);
            }
        } catch (AmazonServiceException ase) {
            StringBuilder sb = new StringBuilder();
            sb.append("Caught an AmazonServiceException, which means your request made it ");
            sb.append("to Amazon S3, but was rejected with an error response for some reason.\n");
            sb.append("Error Message:    " + ase.getMessage());
            sb.append("HTTP Status Code: " + ase.getStatusCode());
            sb.append("AWS Error Code:   " + ase.getErrorCode());
            sb.append("Error Type:       " + ase.getErrorType());
            sb.append("Request ID:       " + ase.getRequestId());
            LOG.error(sb.toString(), ase);
            throw new GenieServerException("Could not get files from S3.", ase);
        } catch (AmazonClientException ace) {
            StringBuilder sb = new StringBuilder();
            sb.append("Caught an AmazonClientException, " +
                    "which means the client encountered " +
                    "an internal error while trying to communicate" +
                    " with S3, " +
                    "such as not being able to access the network.");
            sb.append("Error Message: " + ace.getMessage());
            LOG.error(sb.toString(), ace);
            throw new GenieServerException("Could not get files from S3.", ace);
        }

        return attachments;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Set<FileAttachment> unstoreAttachments(
            final String jobId) throws GenieException {
        final String jobBaseURI = this.getS3JobDir() + jobId + "/";
        final List<S3ObjectSummary> objectList = this.getObjectSummaries(jobBaseURI);
        final HashSet<FileAttachment> attachments = new HashSet<>();

        try {
            for (S3ObjectSummary objInfo : objectList) {
                if (objInfo.getKey().endsWith("/")) {
                    continue;
                }

                LOG.info(String.format("Getting : %s - %s", objInfo.getBucketName(), objInfo.getKey()));
                final S3Object s3Object = this.amazonS3Client.getObject(
                        new GetObjectRequest(objInfo.getBucketName(), objInfo.getKey()));

                // Store in file attachments object & add to the set
                final FileAttachment currObj = new FileAttachment();
                currObj.setName(this.filenameFromKey(s3Object.getKey()));
                currObj.setData(this.getObjectData(s3Object));
                attachments.add(currObj);
            }
        } catch (AmazonServiceException ase) {
            StringBuilder sb = new StringBuilder();
            sb.append("Caught an AmazonServiceException, which means your request made it ");
            sb.append("to Amazon S3, but was rejected with an error response for some reason.\n");
            sb.append("Error Message:    " + ase.getMessage());
            sb.append("HTTP Status Code: " + ase.getStatusCode());
            sb.append("AWS Error Code:   " + ase.getErrorCode());
            sb.append("Error Type:       " + ase.getErrorType());
            sb.append("Request ID:       " + ase.getRequestId());
            LOG.error(sb.toString(), ase);
            throw new GenieServerException("Could not get files from S3.", ase);
        } catch (AmazonClientException ace) {
            StringBuilder sb = new StringBuilder();
            sb.append("Caught an AmazonClientException, " +
                    "which means the client encountered " +
                    "an internal error while trying to communicate" +
                    " with S3, " +
                    "such as not being able to access the network.");
            sb.append("Error Message: " + ace.getMessage());
            LOG.error(sb.toString(), ace);
            throw new GenieServerException("Could not get files from S3.", ace);
        }

        // delete before returning and if an exception occurs just log it
        final AmazonS3URI directoryURI = new AmazonS3URI(jobBaseURI);
        DeleteObjectsRequest multiObjectDeleteRequest = new DeleteObjectsRequest(directoryURI.getBucket());
        List<DeleteObjectsRequest.KeyVersion> keys = new ArrayList<>();
        for (S3ObjectSummary objInfo : objectList) {
            if (objInfo.getKey().endsWith("/")) {
                continue;
            }
            keys.add(new DeleteObjectsRequest.KeyVersion(objInfo.getKey()));
        }

        multiObjectDeleteRequest.setKeys(keys);

        try {
            final DeleteObjectsResult delObjRes = this.amazonS3Client.deleteObjects(multiObjectDeleteRequest);
            LOG.info(String.format("Successfully deleted all the %d items.", delObjRes.getDeletedObjects().size()));
        } catch (MultiObjectDeleteException de) {
            LOG.warn("Could not clean up job directory, will be left on S3.", de);
        } catch (AmazonServiceException ase) {
            StringBuilder sb = new StringBuilder();
            sb.append("Caught an AmazonServiceException trying to delete files from S3.\n");
            sb.append("Error Message:    " + ase.getMessage());
            sb.append("HTTP Status Code: " + ase.getStatusCode());
            sb.append("AWS Error Code:   " + ase.getErrorCode());
            sb.append("Error Type:       " + ase.getErrorType());
            sb.append("Request ID:       " + ase.getRequestId());
            LOG.warn(sb.toString(), ase);
        } catch (AmazonClientException ace) {
            StringBuilder sb = new StringBuilder();
            sb.append("Caught an AmazonClientException trying to delete files from S3.");
            sb.append("Error Message: " + ace.getMessage());
            LOG.warn(sb.toString(), ace);
        }

        return attachments;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void storeAttachments(
            final String jobId,
            final Set<FileAttachment> attachments
    ) throws GenieException {
        if (attachments == null) {
            return; // bail if no attachments
        }

        final String jobBaseURI = this.getS3JobDir() + jobId + "/";

        try {
            for (FileAttachment attachment : attachments) {
                String finalName = jobBaseURI + attachment.getName();
                AmazonS3URI directoryURI = new AmazonS3URI(finalName);

                InputStream inputStream = new ByteArrayInputStream(attachment.getData());
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(attachment.getData().length);

                this.amazonS3Client.putObject(new PutObjectRequest(directoryURI.getBucket(),
                                                                   directoryURI.getKey(), inputStream, metadata));
            }
        } catch (AmazonServiceException ase) {
            StringBuilder sb = new StringBuilder();
            sb.append("Caught an AmazonServiceException, which means your request made it ");
            sb.append("to Amazon S3, but was rejected with an error response for some reason.\n");
            sb.append("Error Message:    " + ase.getMessage());
            sb.append("HTTP Status Code: " + ase.getStatusCode());
            sb.append("AWS Error Code:   " + ase.getErrorCode());
            sb.append("Error Type:       " + ase.getErrorType());
            sb.append("Request ID:       " + ase.getRequestId());
            LOG.error(sb.toString(), ase);
            throw new GenieServerException("Could not store files on S3.", ase);
        } catch (AmazonClientException ace) {
            StringBuilder sb = new StringBuilder();
            sb.append("Caught an AmazonClientException, " +
                    "which means the client encountered " +
                    "an internal error while trying to communicate" +
                    " with S3, " +
                    "such as not being able to access the network.");
            sb.append("Error Message: " + ace.getMessage());
            LOG.error(sb.toString(), ace);
            throw new GenieServerException("Could not store files on S3.", ace);
        }
    }


    /**
     * Retrieve the S3 bucket and base directory to store job artifacts.
     *
     * @return The S3 path with the trailing slash included
     */
    protected String getS3JobDir() {
        final String s3JobDir = CONF.getString("com.netflix.genie.server.s3.jobdir",
                                               "s3://stitchfix.aa/cleanroom/jobtemp/");
        if (s3JobDir.endsWith("/")) {
            return s3JobDir;
        } else {
            return s3JobDir + "/";
        }
    }


    /**
     * Given an S3 key, get the file name by taking the last portion of the path.
     *
     * @param s3Key An S3 key which often looks like a directory + file name
     * @return Returns the file name portion of the key
     */
    protected String filenameFromKey(
            final String s3Key) {
        if (s3Key.contains("/")) {
            return s3Key.substring(s3Key.lastIndexOf('/') + 1);
        } else {
            return s3Key;
        }
    }


    /**
     * Given an S3Object recover its contents as a byte array for a FileAttachment object.
     *
     * @param inputObj The S3 object returned from a getObject call
     * @return The byte array of its contents
     * @throws GenieServerException If any IO errors occur
     */
    protected byte[] getObjectData(
            final S3Object inputObj) throws GenieServerException {
        try (S3ObjectInputStream is = inputObj.getObjectContent()) {
            byte[] bytes = ByteStreams.toByteArray(is);
            return bytes;
        } catch (IOException iox) {
            throw new GenieServerException("Error reading object contents.", iox);
        }
    }


    /**
     * Call S3 to get an object list from within a bucket URI.
     *
     * This is an object list request to AWS in the given "directory", after which we can retrieve
     * the individual file objects.
     *
     * @return Returns objects in the given "folder" defined by the URI
     */
    protected List<S3ObjectSummary> getObjectSummaries(
            final String bucketUri) throws GenieServerException {
        final AmazonS3URI directoryURI = new AmazonS3URI(bucketUri);

        ArrayList<S3ObjectSummary> returnList = new ArrayList<S3ObjectSummary>();
        try {
            LOG.info(String.format("Getting the listing of job files from: bucket %s key %s :",
                                   directoryURI.getBucket(), directoryURI.getKey()));
            final ListObjectsRequest req = new ListObjectsRequest().withBucketName(directoryURI.getBucket()).
                    withPrefix(directoryURI.getKey()).withDelimiter("/").withMaxKeys(25);
            ObjectListing result;

            do {
                result = this.amazonS3Client.listObjects(req);

                returnList.addAll(result.getObjectSummaries());
                req.setMarker(result.getNextMarker());
            }
            while(result.isTruncated());

            LOG.info("Completed getting S3 object listing.");
        } catch (AmazonServiceException ase) {
            StringBuilder sb = new StringBuilder();
            sb.append("Caught an AmazonServiceException, which means your request made it ");
            sb.append("to Amazon S3, but was rejected with an error response for some reason.\n");
            sb.append("Error Message:    " + ase.getMessage());
            sb.append("HTTP Status Code: " + ase.getStatusCode());
            sb.append("AWS Error Code:   " + ase.getErrorCode());
            sb.append("Error Type:       " + ase.getErrorType());
            sb.append("Request ID:       " + ase.getRequestId());
            LOG.error(sb.toString(), ase);
            throw new GenieServerException("Could not list files in S3.", ase);
        } catch (AmazonClientException ace) {
            StringBuilder sb = new StringBuilder();
            sb.append("Caught an AmazonClientException, " +
                    "which means the client encountered " +
                    "an internal error while trying to communicate" +
                    " with S3, " +
                    "such as not being able to access the network.");
            sb.append("Error Message: " + ace.getMessage());
            LOG.error(sb.toString(), ace);
            throw new GenieServerException("Could not list files in S3.", ace);
        }

        return returnList;
    }
}
