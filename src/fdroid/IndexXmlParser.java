/*
 * Copyright 2013 Thomas Pilot
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fdroid;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

// parser currently not in use, the APKs in ./fdroid/ are used directly
public class IndexXmlParser {
	
	public static final String REPO_PREFIX = "http://f-droid.org/repo/";
	
	public static void main(String[] args) {
		String[] apks = getLatestApks();
		for (String apk : apks) {
			System.out.println(apk);
		}
	}
	
	public static String[] getLatestApks() {
		String indexXmlUri = REPO_PREFIX + "index.xml";
		Document doc = null;
		try {
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			doc = docBuilder.parse(indexXmlUri);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new RuntimeException("parsing of " + indexXmlUri + " failed", e);
		}
		NodeList applications = doc.getElementsByTagName("application");
		return getApkList(applications);
	}

	private static String[] getApkList(NodeList applications) {
		String[] apks = new String[applications.getLength()];
		for (int appIdx = 0; appIdx < apks.length; appIdx++) {
			Element app = (Element) applications.item(appIdx);
			NodeList packs = app.getElementsByTagName("package");
			Element pack = getLatestPack(packs);
			String apkName = getText(pack, "apkname");
			apks[appIdx] = REPO_PREFIX + apkName;
		}
		return apks;
	}

	private static Element getLatestPack(NodeList packs) {
		Element latestPack = null;
		String latestVersion = "";
		for (int packIdx = 0; packIdx < packs.getLength(); packIdx++) {
			Element pack = (Element) packs.item(packIdx);
			String packVersion = getText(pack, "version");
			if (packVersion.compareTo(latestVersion) > 0) {
				latestVersion = packVersion;
				latestPack = pack;
			}
		}
		if (latestPack == null) {
			throw new RuntimeException("no latest pack found");
		}
		return latestPack;
	}
	
	private static String getText(Element e, String tagName) {
		NodeList elements = e.getElementsByTagName(tagName);
		Node firstItem = elements.item(0);
		if (firstItem == null) {
			throw new RuntimeException("tag " + tagName + " not found in element " + e);
		}
		return firstItem.getFirstChild().getNodeValue();
	}
}