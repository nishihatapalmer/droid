/*
 * Copyright (c) 2016, The National Archives <pronom@nationalarchives.gov.uk>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of the The National Archives nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package uk.gov.nationalarchives.droid.signature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.hamcrest.Matchers;
import org.junit.*;
import org.mockito.ArgumentCaptor;

import uk.gov.nationalarchives.droid.core.interfaces.config.DroidGlobalConfig;
import uk.gov.nationalarchives.droid.core.interfaces.config.DroidGlobalProperty;
import uk.gov.nationalarchives.droid.core.interfaces.signature.SignatureFileInfo;
import uk.gov.nationalarchives.droid.core.interfaces.signature.SignatureServiceException;
import uk.gov.nationalarchives.droid.core.interfaces.signature.SignatureType;
import uk.gov.nationalarchives.droid.core.interfaces.signature.SignatureUpdateService;
import uk.gov.nationalarchives.droid.util.FileUtil;

/**
 * @author rflitcroft
 *
 */
public class SignatureManagerImplTest {

    private static Path containerSigFileDir;
    private static Path textSigFileDir;
    
    private SignatureManagerImpl signatureManager;
    private DroidGlobalConfig config;
    private PropertiesConfiguration configuration;
    private SignatureUpdateService signatureService;
    private SignatureUpdateService containerSignatureService;
    private SignatureUpdateService textSignatureService;
    
    @Before
    public void initTestFiles() {
        createSigDirs();
    }
    
    @After
    public void tearDownTestFiles() {
        deleteSigDirs();
    }
    
    private static void createSigDirs() {
        containerSigFileDir = Paths.get("test_container_sig_files");
        FileUtil.mkdirsQuietly(containerSigFileDir);

        textSigFileDir = Paths.get("test_text_sig_files");
        FileUtil.mkdirsQuietly(textSigFileDir);
    }
    
    private static void deleteSigDirs() {
        FileUtil.deleteQuietly(containerSigFileDir);
        FileUtil.deleteQuietly(textSigFileDir);
    }
    
    @Before
    public void setup() {
        signatureManager = new SignatureManagerImpl();
        config = mock(DroidGlobalConfig.class);
        when(config.getSignatureFileDir()).thenReturn(Paths.get("test_sig_files"));
        when(config.getContainerSignatureDir()).thenReturn(containerSigFileDir);
        when(config.getTextSignatureFileDir()).thenReturn(textSigFileDir);
        
        configuration = mock(PropertiesConfiguration.class);
        when(config.getProperties()).thenReturn(configuration);
        signatureManager.setConfig(config);
        
        signatureService = mock(SignatureUpdateService.class);
        containerSignatureService = mock(SignatureUpdateService.class);
        textSignatureService = mock(SignatureUpdateService.class);
        
        Map<SignatureType, SignatureUpdateService> updateServices = 
            new HashMap<SignatureType, SignatureUpdateService>();
        updateServices.put(SignatureType.BINARY, signatureService);
        updateServices.put(SignatureType.CONTAINER, containerSignatureService);
        //updateServices.put(SignatureType.TEXT, textSignatureService);
        
        signatureManager.setSignatureUpdateServices(updateServices);
    }
    
    @Test
    public void testGetAvailableSignatureFiles() {
        
        
        Map<SignatureType, SortedMap<String, SignatureFileInfo>> infos = signatureManager.getAvailableSignatureFiles();
        Map<String, SignatureFileInfo> binaryInfos = infos.get(SignatureType.BINARY);
        assertEquals(16, binaryInfos.get("DROID_SignatureFile_V16").getVersion());
        assertEquals(26, binaryInfos.get("DROID_SignatureFile_V26").getVersion());
        assertEquals(16, binaryInfos.get("malformed").getVersion());

        assertEquals(2, infos.size());
    }

    /*
    //TODO:MP: failing from command line
     */
    @Ignore
    @Test
    public void testCheckForNewSignatureFileWhenNewFileIsNotAvailable() throws SignatureServiceException {
        
        SignatureFileInfo signatureFileInfo = new SignatureFileInfo(26, false, SignatureType.BINARY);
        when(signatureService.getLatestVersion(20100101)).thenReturn(signatureFileInfo);
        when(containerSignatureService.getLatestVersion(20100101)).thenReturn(null);
        //when(textSignatureService.getLatestVersion(20100101)).thenReturn(null);
        
        Map<SignatureType, SignatureFileInfo> newSignatures = signatureManager.getLatestSignatureFiles();
        final SignatureFileInfo newSignature = newSignatures.get(SignatureType.BINARY);
        assertNull(newSignature);
        
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(configuration).setProperty(eq(DroidGlobalProperty.LAST_UPDATE_CHECK.getName()), 
                captor.capture());
        
        assertThat((double) captor.getValue(), Matchers.closeTo(System.currentTimeMillis(), 200L));

    }

    @Test
    @Ignore 
 /* This test is broken from within TNA. This was previously masked by
 *  setting the hardcoded latest version to the actual latest version (then
 *  26) 
 */
    public void testCheckForNewSignatureFileWhenNewFileIsAvailable() throws SignatureServiceException {
        
        SignatureFileInfo signatureFileInfo = new SignatureFileInfo(100, false, SignatureType.BINARY);
        
        when(signatureService.getLatestVersion(26)).thenReturn(signatureFileInfo);
        
        Map<SignatureType, SignatureFileInfo> newSignatures = signatureManager.getLatestSignatureFiles();
        assertNotNull(newSignatures.get(SignatureType.BINARY));
        assertEquals(signatureFileInfo, newSignatures.get(SignatureType.BINARY));
        
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(configuration).setProperty(eq(DroidGlobalProperty.LAST_UPDATE_CHECK.getName()), 
                captor.capture());
        
        assertThat((double) captor.getValue(), Matchers.closeTo(System.currentTimeMillis(), 1000L));
        
    }
    
    @Test
    public void testDownloadLatest() throws Exception {
        
        signatureManager.downloadLatest(SignatureType.BINARY);
        verify(signatureService).importSignatureFile(config.getSignatureFileDir());
    }
    
    @Test
    public void testGetAvailableContainerSignatureFiles() throws IOException {
        
        deleteSigDirs();
        createSigDirs();
        
        final Path containerSigFile1 = containerSigFileDir.resolve("container_sigs-20100927.xml");
        final Path containerSigFile2 = containerSigFileDir.resolve("container_sigs-20100827.xml");
        
        Files.createFile(containerSigFile1);
        Files.createFile(containerSigFile2);
        
        assertTrue(Files.exists(containerSigFile1));
        assertTrue(Files.exists(containerSigFile2));
        
        Map<SignatureType, SortedMap<String, SignatureFileInfo>> sigFiles = 
            signatureManager.getAvailableSignatureFiles();
        SortedMap<String, SignatureFileInfo> containerSigFiles = sigFiles.get(SignatureType.CONTAINER);
        
        
        assertEquals(2, containerSigFiles.size());
        
        assertEquals(containerSigFile1, containerSigFiles.get("container_sigs-20100927").getFile());
        assertEquals(containerSigFile2, containerSigFiles.get("container_sigs-20100827").getFile());

        assertEquals(containerSigFileDir.resolve("container_sigs-20100927.xml"),
                containerSigFiles.get("container_sigs-20100927").getFile());
    }
    
    @Test
    public void testGetDefaultContainerSignature() throws Exception {
        final Path containerSigFile1 = containerSigFileDir.resolve("container_sigs-20100927.xml");
        final Path containerSigFile2 = containerSigFileDir.resolve("container_sigs-20100827.xml");
        
        final Path textSigFile1 = textSigFileDir.resolve("text_sigs-20110927.xml");
        final Path textSigFile2 = textSigFileDir.resolve("text_sigs-20110827.xml");

        Files.createFile(containerSigFile1);
        Files.createFile(containerSigFile2);
        Files.createFile(textSigFile1);
        Files.createFile(textSigFile2);
        
        assertTrue(Files.exists(containerSigFile1));
        assertTrue(Files.exists(containerSigFile2));
        assertTrue(Files.exists(textSigFile1));
        assertTrue(Files.exists(textSigFile2));
        
        when(configuration.getString(DroidGlobalProperty.DEFAULT_BINARY_SIG_FILE_VERSION.getName()))
            .thenReturn("DROID_SignatureFile_V26");
        when(configuration.getString(DroidGlobalProperty.DEFAULT_CONTAINER_SIG_FILE_VERSION.getName()))
            .thenReturn("container_sigs-20100927");
        when(configuration.getString(DroidGlobalProperty.DEFAULT_TEXT_SIG_FILE_VERSION.getName()))
            .thenReturn("text_sigs-20110827");
        
        SignatureFileInfo sigInfo = signatureManager.getDefaultSignatures().get(SignatureType.CONTAINER);
        assertEquals(containerSigFileDir.resolve("container_sigs-20100927.xml"), sigInfo.getFile());
        assertEquals(20100927, sigInfo.getVersion());
    }
    
    @Test
    public void testGetLatestContainerSignatureFileWhenCurrentFileIsLatest() throws Exception {
        
        SignatureFileInfo latestContainerSigInfo = new SignatureFileInfo(20100929, false, SignatureType.CONTAINER);
        when(containerSignatureService.getLatestVersion(20100101)).thenReturn(latestContainerSigInfo);

        deleteSigDirs();
        createSigDirs();
        
        final Path containerSigFile1 = containerSigFileDir.resolve("container_sigs-20100927.xml");
        final Path containerSigFile2 = containerSigFileDir.resolve("container_sigs-20100929.xml");

        Files.createFile(containerSigFile1);
        Files.createFile(containerSigFile2);
        
        assertTrue(Files.exists(containerSigFile1));
        assertTrue(Files.exists(containerSigFile2));

        SignatureFileInfo sigFileInfo = signatureManager.getLatestSignatureFiles().get(SignatureType.CONTAINER);
        assertNull(sigFileInfo);
    }
    
    @Test
    public void testGetLatestConatinerSignatureFileWhenCurrentFileIsOutOfDate() throws Exception {
        SignatureFileInfo latestContainerSigInfo = new SignatureFileInfo(20100930, false, SignatureType.CONTAINER);
        when(containerSignatureService.getLatestVersion(20100828)).thenReturn(latestContainerSigInfo);
        
        deleteSigDirs();
        createSigDirs();
        
        final Path containerSigFile1 = containerSigFileDir.resolve("container_sigs-20100827.xml");
        final Path containerSigFile2 = containerSigFileDir.resolve("container_sigs-20100828.xml");

        Files.createFile(containerSigFile1);
        Files.createFile(containerSigFile2);
        
        assertTrue(Files.exists(containerSigFile1));
        assertTrue(Files.exists(containerSigFile2));

        SignatureFileInfo sigFileInfo = signatureManager.getLatestSignatureFiles().get(SignatureType.CONTAINER);
        assertEquals(latestContainerSigInfo, sigFileInfo);
    }

}
