/*
 * Copyright 2014 The British Library/SCAPE Project Consortium
 * Authors: William Palmer (William.Palmer@bl.uk)
 *          Alecs Geuder (Alecs.Geuder@bl.uk)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package au.gov.nla.flint.wrappers;

import org.apache.commons.compress.PasswordRequiredException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccess;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.preflight.PreflightDocument;
import org.apache.pdfbox.preflight.ValidationResult;
import org.apache.pdfbox.preflight.exception.SyntaxValidationException;
import org.apache.pdfbox.preflight.parser.PreflightParser;
import org.apache.pdfbox.preflight.parser.XmlResultParser;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to wrap the Apache PDFBox library
 */
public class PDFBoxWrapper {

    private static Logger LOGGER = LoggerFactory.getLogger(PDFBoxWrapper.class);

    private final Map<String, Element> pseudoCache = new HashMap<String, Element>();

    private final XmlResultParser parser = new CachingXmlResultParser();

    public PDFBoxWrapper() {}

    /**
     * As preflight is used more than once for different purposes the result
     * shall be cached for performance reasons.
     */
    private class CachingXmlResultParser extends XmlResultParser {
        public Element validate (Document rdocument, File source) throws IOException {
            synchronized (this.getClass()) {
                if (pseudoCache.containsKey(source.getName())) {
                    // can be null, which means it's not valid
                    Element preflight = pseudoCache.get(source.getName());
                    // we only cache ONCE and clear after.
                    pseudoCache.clear();
                    return preflight;
                }
            }
            // state that one has dealt with this file:
            pseudoCache.put(source.getName(), null);

            // now do the actual work that will finish with caching and returning the element
            // in case something goes wrong, the empty entry above will remain testifying we have tried.
            String pdfType = null;
            ValidationResult result = null;
            long before = System.currentTimeMillis();
            PreflightDocument document = null;
            try {
                LOGGER.debug("Beginning the preflight validation.. of {}", source.getName());
                result = PreflightParser.validate(source);
            } catch (SyntaxValidationException e) {
                result = e.getResult();
            } finally {
                if (document != null) document.close();
            }
            long after = System.currentTimeMillis();

            LOGGER.debug("generate-response-skeleton");
            Element preflight = generateResponseSkeleton(rdocument, source.getName(), after-before);
            if (result != null && result.isValid()) {
                // valid ?
                Element valid = rdocument.createElement("isValid");
                valid.setAttribute("type", pdfType);
                valid.setTextContent("true");
                preflight.appendChild(valid);
            } else {
                // valid ?
                createResponseWithError(rdocument, pdfType, result, preflight);
            }
            pseudoCache.put(source.getName(), preflight);
            return preflight;
        }
    }

    /**
     * Runs preflight over the pdf file and produces an output file.
     * If the transformation of the preflight output Element to xml
     * @param pFile the input file
     * @return the output-stream of the preflight validation (null if errors occurred).
     * @throws IOException
     * @throws TransformerException 
     */
    public ByteArrayOutputStream preflightToXml(File pFile) throws IOException, TransformerException {
        Element result = parser.validate(pFile);
        LOGGER.debug("generating xml from preflight generated element for {}", pFile);
        Document doc = result.getOwnerDocument();
        doc.appendChild(result);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(output));
        return output;
    }

    /**
     * A better PDFBox isValid() method
     * @param pFile file to check
     * @return true if valid, false if not
     */
    public boolean isValid(File pFile) {
        try {
            if (parser.validate(pFile) == null) {
                return false;
            }
        } catch (IOException e) {
            LOGGER.warn("IOException leads to invalidity: {}", e);
            return false;
        } catch (IllegalArgumentException e) {
            LOGGER.warn("IllegalArgumentException leads to invalidity: {}", e);
            return false;
        } catch (Exception e) {
            LOGGER.warn("Exception leads to invalidity: {}", e);
            return false;
        }

        return true;
    }

	/**
	 * Check if a PDF file has DRM or not
	 * @param pFile file to check
	 * @return whether the file is had DRM or not
	 */
	public boolean hasDRM(File pFile) {
		boolean ret = false;
		
		try {
			System.setProperty("org.apache.pdfbox.baseParser.pushBackSize", "1024768");
			PDDocument doc = Loader.loadPDF(pFile);
			ret = doc.isEncrypted();
			doc.close();

		} catch (InvalidPasswordException e) {
			ret = true;
		}
		catch(IOException e) {

			// This may occur when a suitable security handler cannot be found
			if(e.getMessage().contains("BadSecurityHandlerException")) {
				// if this happens then there must be some sort of DRM here
				ret = true;
			}

		} catch (Exception e) {

			e.printStackTrace();

			// See comments in https://issues.apache.org/jira/browse/PDFBOX-1757
			// PDFBox state that these files have errors and their parser is correct
			// The only way to find out that the parser doesn't like it is to catch
			// a general Exception.

			// If we reach this point then we have no idea of whether the file contains
			// DRM or not.  Return false and hope it is detected elsewhere.

			ret = false;
		}
		return ret;
	}



	/**
	 * Extracts text from a PDF.  Note that Tika uses PDFBox so we will just use the library directly and avoid waiting for
	 * Tika to use the latest version.
	 * Inspired by PDFBox's ExtractText.java
	 * @param pFile input file
	 * @param pOutput output file
	 * @param pOverwrite whether or not to overwrite an existing output file
	 * @return true if converted ok, otherwise false
	 */
	public boolean extractTextFromPDF(File pFile, File pOutput, boolean pOverwrite) {
		if(pOutput.exists()&(!pOverwrite)) return false;
        PDDocument doc = null;
        PrintWriter out = null;
		try {
			PDFTextStripper ts = new PDFTextStripper();
			out = new PrintWriter(new FileWriter(pOutput));
			boolean skipErrors = true;
			doc = Loader.loadPDF(pFile);
			ts.writeText(doc, out);
			// TODO: extract text from embedded files?
			return true;
		} catch (OutOfMemoryError e) {
            LOGGER.error("out of memory error while trying to extract text from file {}! : {}", pFile.getName(), e);
            System.gc();
        } catch (Exception e) {
			// TODO Auto-generated catch block
            LOGGER.error("caught Exception: {}", e);
			e.printStackTrace();
		} finally {
            try {
                out.close();
                if (doc != null) doc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
		return false;

	}

}
