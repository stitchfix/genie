package com.netflix.genie.server.services.impl.s3;

import com.netflix.genie.common.exceptions.GenieException;
import com.netflix.genie.common.model.FileAttachment;
import com.netflix.genie.server.services.FileAttachmentStorage;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

/**
 * Unit test for S3 based file attachment storage.
 *
 * Created by derekbennett on 1/11/17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:genie-application-test.xml")
public class TestFileAttachmentStorageS3 {

    @Inject
    private FileAttachmentStorage jobAttachmentStorage;


    /**
     * Test other support methods used by the implementation class.
     *
     * @throws GenieException If any exception occurs
     */
    @Test
    public void testSupportMethods() throws GenieException {
        FileAttachmentStorageS3 storageImpl = new FileAttachmentStorageS3();

        final String returnedJobDir = storageImpl.getS3JobDir();
        Assert.assertNotNull(returnedJobDir);
        Assert.assertTrue(returnedJobDir.endsWith("/"));

        final String key1 = "cleanroom/jobs/abc123/sparkJob.py";
        final String key2 = "stdout.log";
        final String key3 = "abc123/driver.py";

        Assert.assertEquals("sparkJob.py", storageImpl.filenameFromKey(key1));
        Assert.assertEquals("stdout.log", storageImpl.filenameFromKey(key2));
        Assert.assertEquals("driver.py", storageImpl.filenameFromKey(key3));
    }


    //// Note: these are more integration tests that use S3


    /**
     * Test a job that we know does not exist and ensure we don't blow up.
     *
     * @throws GenieException More superfluous comments
     */
    @Test
    public void testUnknownJob() throws GenieException {
        final String jobId = "unknown-abcd-1234";
        final Set<FileAttachment> attachments = jobAttachmentStorage.getAttachments(jobId);
        Assert.assertNotNull(attachments);
        Assert.assertTrue(attachments.isEmpty());
    }


    /**
     * Test storing some data on S3 and then getting it back to make sure all is OK with the round trip.
     *
     * @throws GenieException Throws exception for any issues
     * @throws UnsupportedEncodingException A superfluous comment required by style checker
     */
    @Test
    public void testStoreAndGet() throws GenieException, UnsupportedEncodingException {
        final String jobId = "test-job-1234";
        HashSet<FileAttachment> attachments = new HashSet<>();
        FileAttachment fa = new FileAttachment();
        fa.setName("dataFile.txt");
        String fakeData = "# Sample properties file\n" +
                "archaius_version=0.6.5\n\n" +
                "com.netflix.genie.server.max.system.jobs=35\n" +
                "com.netflix.genie.job.max.stdout.size=8589934592\n";
        fa.setData(fakeData.getBytes("UTF-8"));
        attachments.add(fa);

        jobAttachmentStorage.storeAttachments(jobId, attachments);

        Set<FileAttachment> returnedBack = jobAttachmentStorage.getAttachments(jobId);
        Assert.assertNotNull(returnedBack);
        Assert.assertFalse(returnedBack.isEmpty());
        Assert.assertEquals(1, returnedBack.size());
        FileAttachment returned = returnedBack.iterator().next();
        Assert.assertEquals(fa.getName(), returned.getName());
        Assert.assertArrayEquals(fa.getData(), returned.getData());
    }


    /**
     * Test storing some data on S3 and then getting it back using unstore.
     *
     * @throws GenieException Throws exception for any issues
     * @throws UnsupportedEncodingException A superfluous comment required by style checker
     */
    @Test
    public void testStoreAndUnstore() throws GenieException, UnsupportedEncodingException {
        final String jobId = "test-job-5678";
        HashSet<FileAttachment> attachments = new HashSet<>();
        FileAttachment fa = new FileAttachment();
        fa.setName("driver.py");
        String fakeData = "import logging\n" +
                "import sys\n" +
                "from sfspark.sfcontext import AASparkContext, setup_logging\n" +
                "if __name__ == \"__main__\":\n" +
                "    setup_logging()\n" +
                "    logger = logging.getLogger(\"web_client_event_backfill\")\n";
        fa.setData(fakeData.getBytes("UTF-8"));
        attachments.add(fa);

        jobAttachmentStorage.storeAttachments(jobId, attachments);

        // Note: deletion may not be allowed
        Set<FileAttachment> returnedBack = jobAttachmentStorage.unstoreAttachments(jobId);
        Assert.assertNotNull(returnedBack);
        Assert.assertFalse(returnedBack.isEmpty());
        Assert.assertEquals(1, returnedBack.size());
        FileAttachment returned = returnedBack.iterator().next();
        Assert.assertEquals(fa.getName(), returned.getName());
        Assert.assertArrayEquals(fa.getData(), returned.getData());
    }
}
