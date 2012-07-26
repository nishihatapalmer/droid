/**
 * <p>Copyright (c) The National Archives 2005-2010.  All rights reserved.
 * See Licence.txt for full licence details.
 * <p/>
 *
 * <p>DROID DCS Profile Tool
 * <p/>
 */
package uk.gov.nationalarchives.droid.command.container;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import uk.gov.nationalarchives.droid.command.action.CommandExecutionException;
import uk.gov.nationalarchives.droid.container.*;
import uk.gov.nationalarchives.droid.container.zip.ZipIdentifierEngine;
import uk.gov.nationalarchives.droid.core.SignatureParseException;
import uk.gov.nationalarchives.droid.core.interfaces.IdentificationResult;
import uk.gov.nationalarchives.droid.core.interfaces.IdentificationResultCollection;
import uk.gov.nationalarchives.droid.core.interfaces.RequestIdentifier;
import uk.gov.nationalarchives.droid.core.interfaces.resource.FileSystemIdentificationRequest;
import uk.gov.nationalarchives.droid.core.interfaces.resource.RequestMetaData;

/**
 *
 * @author rbrennan
 */
public class ZipContainerContentIdentifierTest {
    
    private ZipContainerContentIdentifier zipContainerContentIdentifier;
    private ZipIdentifierEngine zipIdentifierEngine;
    private ContainerFileIdentificationRequestFactory requestFactory;
    private ContainerIdentifierInit containerIdentifierInit;
    private ContainerSignatureDefinitions containerSignatureDefinitions;
    private Map<Integer, List<FileFormatMapping>> formats; 
    private String containerSignatures =
            "../droid-container/workDir/container-sigs/container-signature.xml";
    private String zipFile =
            "../droid-container/src/test/resources/word_ooxml.docx";
    
    @Before
    public void setUp() {
        zipContainerContentIdentifier = new ZipContainerContentIdentifier();
        requestFactory = new ContainerFileIdentificationRequestFactory();
        zipIdentifierEngine = new ZipIdentifierEngine();
        containerIdentifierInit = new ContainerIdentifierInit();
        formats = new HashMap<Integer, List<FileFormatMapping>>();
    }
    
    @After
    public void tearDown() {
        zipContainerContentIdentifier = null;
        requestFactory = null;
        zipIdentifierEngine = null;
        containerIdentifierInit = null;
        formats = null;
    }
    
    @Test
    public void identifyZipFileTest() throws CommandExecutionException {

        zipIdentifierEngine.setRequestFactory(requestFactory);
        zipContainerContentIdentifier.setIdentifierEngine(zipIdentifierEngine);
        Object object = zipContainerContentIdentifier.getIdentifierEngine();
        assertEquals(object.getClass(), zipIdentifierEngine.getClass());
        
        try {
            InputStream in = new FileInputStream(containerSignatures);
            ContainerSignatureSaxParser parser = new ContainerSignatureSaxParser();
            containerSignatureDefinitions = parser.parse(in);
        } catch (SignatureParseException e) {
            throw new CommandExecutionException ("Can't parse container signature file");
        } catch (Exception e) {
            throw new CommandExecutionException(e);
        }
        
        zipContainerContentIdentifier.init(containerSignatureDefinitions, "ZIP");
        object = zipContainerContentIdentifier.getContainerIdentifierInit();
        assertEquals(object.getClass(), containerIdentifierInit.getClass());
        
        object = zipContainerContentIdentifier.getFormats();
        assertEquals(object.getClass(), formats.getClass());

        String fileName;
        File file = new File(zipFile);
        if (!file.exists()) {
            fail("ZIP test file not found");
        }
        URI uri = file.toURI();
        RequestIdentifier identifier = new RequestIdentifier(uri);
        identifier.setParentId(1L);
        try {
            fileName = file.getCanonicalPath();
            RequestMetaData metaData =
                new RequestMetaData(file.length(), file.lastModified(), fileName);
            FileSystemIdentificationRequest request =
                new FileSystemIdentificationRequest(metaData, identifier);
            IdentificationResultCollection results =
                new IdentificationResultCollection(request);
            InputStream zipStream = new FileInputStream(file);
            request.open(zipStream);
            results = zipContainerContentIdentifier.process(request.getSourceInputStream(), results);
            if (results.getResults().isEmpty()) {
                fail("ZIP file not identified");
            }
            String fmtExpected = "";
            for (IdentificationResult identResult : results.getResults()) {
                if (identResult.getPuid().equals("fmt/189")) {
                    fmtExpected = identResult.getPuid();
                }
            }
            assertEquals(fmtExpected, "fmt/189");
        } catch (IOException e) {
            throw new CommandExecutionException(e);
        }
    }
}