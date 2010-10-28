/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/xml/sitemap/Attic/CmsXmlSitemap.java,v $
 * Date   : $Date: 2010/10/28 07:38:56 $
 * Version: $Revision: 1.40 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) 2002 - 2009 Alkacon Software GmbH (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.xml.sitemap;

import org.opencms.file.CmsFile;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsProperty;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.i18n.CmsEncoder;
import org.opencms.i18n.CmsLocaleManager;
import org.opencms.lock.CmsLock;
import org.opencms.main.CmsEvent;
import org.opencms.main.CmsException;
import org.opencms.main.CmsIllegalArgumentException;
import org.opencms.main.CmsLog;
import org.opencms.main.CmsRuntimeException;
import org.opencms.main.I_CmsEventListener;
import org.opencms.main.OpenCms;
import org.opencms.relations.CmsLink;
import org.opencms.relations.CmsRelationType;
import org.opencms.util.CmsCollectionsGenericWrapper;
import org.opencms.util.CmsMacroResolver;
import org.opencms.util.CmsStringUtil;
import org.opencms.util.CmsUUID;
import org.opencms.xml.CmsXmlContentDefinition;
import org.opencms.xml.CmsXmlException;
import org.opencms.xml.CmsXmlGenericWrapper;
import org.opencms.xml.CmsXmlUtils;
import org.opencms.xml.content.CmsXmlContent;
import org.opencms.xml.content.CmsXmlContentMacroVisitor;
import org.opencms.xml.content.CmsXmlContentProperty;
import org.opencms.xml.content.CmsXmlContentPropertyHelper;
import org.opencms.xml.page.CmsXmlPage;
import org.opencms.xml.sitemap.properties.CmsSimplePropertyValue;
import org.opencms.xml.types.CmsXmlNestedContentDefinition;
import org.opencms.xml.types.CmsXmlVfsFileValue;
import org.opencms.xml.types.I_CmsXmlContentValue;
import org.opencms.xml.types.I_CmsXmlSchemaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.xml.sax.EntityResolver;

/**
 * Implementation of a object used to access and manage the xml data of a sitemaps.<p>
 * 
 * In addition to the XML content interface. It also provides access to more comfortable beans. 
 * 
 * @author Michael Moossen 
 * 
 * @version $Revision: 1.40 $ 
 * 
 * @since 7.5.2
 * 
 * @see #getSitemap(CmsObject, Locale)
 */
public class CmsXmlSitemap extends CmsXmlContent {

    /** XML node name constants. */
    public enum XmlNode {

        /** Entry ID node name. */
        Id,
        /** Entry name node name. */
        Name,
        /** Site entry node name. */
        SiteEntry,
        /** Title node name. */
        Title,
        /** Vfs File node name. */
        VfsFile;
    }

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsXmlSitemap.class);

    /** The list of elements corresponding to newly created sitemap entries. */
    private List<Element> m_newEntryElements = new ArrayList<Element>();

    /** The sitemap objects. */
    private Map<Locale, CmsSitemapBean> m_sitemaps;

    /**
     * Hides the public constructor.<p>
     */
    protected CmsXmlSitemap() {

        // noop
    }

    /**
     * Creates a new sitemap based on the provided XML document.<p>
     * 
     * The given encoding is used when marshalling the XML again later.<p>
     * 
     * @param cms the cms context, if <code>null</code> no link validation is performed 
     * @param document the document to create the sitemap from
     * @param encoding the encoding of the sitemap
     * @param resolver the XML entity resolver to use
     */
    protected CmsXmlSitemap(CmsObject cms, Document document, String encoding, EntityResolver resolver) {

        // must set document first to be able to get the content definition
        m_document = document;
        // for the next line to work the document must already be available
        m_contentDefinition = getContentDefinition(resolver);
        // initialize the XML content structure
        initDocument(cms, m_document, encoding, m_contentDefinition);
    }

    /**
     * Create a new sitemap based on the given default content,
     * that will have all language nodes of the default content and ensures the presence of the given locale.<p> 
     * 
     * The given encoding is used when marshalling the XML again later.<p>
     * 
     * @param cms the current users OpenCms content
     * @param locale the locale to generate the default content for
     * @param modelUri the absolute path to the sitemap file acting as model
     * 
     * @throws CmsException in case the model file is not found or not valid
     */
    protected CmsXmlSitemap(CmsObject cms, Locale locale, String modelUri)
    throws CmsException {

        // init model from given modelUri
        CmsFile modelFile = cms.readFile(modelUri, CmsResourceFilter.ONLY_VISIBLE_NO_DELETED);
        CmsXmlSitemap model = CmsXmlSitemapFactory.unmarshal(cms, modelFile);

        // initialize macro resolver to use on model file values
        CmsMacroResolver macroResolver = CmsMacroResolver.newInstance().setCmsObject(cms);

        // content definition must be set here since it's used during document creation
        m_contentDefinition = model.getContentDefinition();
        // get the document from the default content
        Document document = (Document)model.m_document.clone();
        // initialize the XML content structure
        initDocument(cms, document, model.getEncoding(), m_contentDefinition);
        // resolve eventual macros in the nodes
        visitAllValuesWith(new CmsXmlContentMacroVisitor(cms, macroResolver));
        if (!hasLocale(locale)) {
            // required locale not present, add it
            try {
                addLocale(cms, locale);
            } catch (CmsXmlException e) {
                // this can not happen since the locale does not exist
            }
        }
    }

    /**
     * Create a new sitemap based on the given content definition,
     * that will have one language node for the given locale all initialized with default values.<p> 
     * 
     * The given encoding is used when marshalling the XML again later.<p>
     * 
     * @param cms the current users OpenCms content
     * @param locale the locale to generate the default content for
     * @param encoding the encoding to use when marshalling the sitemap later
     * @param contentDefinition the content definition to create the content for
     */
    protected CmsXmlSitemap(CmsObject cms, Locale locale, String encoding, CmsXmlContentDefinition contentDefinition) {

        // content definition must be set here since it's used during document creation
        m_contentDefinition = contentDefinition;
        // create the XML document according to the content definition
        Document document = m_contentDefinition.createDocument(cms, this, locale);
        // initialize the XML content structure
        initDocument(cms, document, encoding, m_contentDefinition);
    }

    /**
     * No validation since rescursive schema.<p>
     * 
     * @see org.opencms.xml.content.CmsXmlContent#addValue(org.opencms.file.CmsObject, java.lang.String, java.util.Locale, int)
     */
    @Override
    public I_CmsXmlContentValue addValue(CmsObject cms, String path, Locale locale, int index)
    throws CmsIllegalArgumentException, CmsRuntimeException {

        // get the schema type of the requested path           
        I_CmsXmlSchemaType type = m_contentDefinition.getSchemaType(path);
        if (type == null) {
            throw new CmsIllegalArgumentException(org.opencms.xml.content.Messages.get().container(
                org.opencms.xml.content.Messages.ERR_XMLCONTENT_UNKNOWN_ELEM_PATH_SCHEMA_1,
                path));
        }

        Element parentElement;
        String elementName;
        CmsXmlContentDefinition contentDefinition;
        if (CmsXmlUtils.isDeepXpath(path)) {
            // this is a nested content definition, so the parent element must be in the bookmarks
            String parentPath = CmsXmlUtils.createXpath(CmsXmlUtils.removeLastXpathElement(path), 1);
            Object o = getBookmark(parentPath, locale);
            if (o == null) {
                throw new CmsIllegalArgumentException(org.opencms.xml.content.Messages.get().container(
                    org.opencms.xml.content.Messages.ERR_XMLCONTENT_UNKNOWN_ELEM_PATH_1,
                    path));
            }
            CmsXmlNestedContentDefinition parentValue = (CmsXmlNestedContentDefinition)o;
            parentElement = parentValue.getElement();
            elementName = CmsXmlUtils.getLastXpathElement(path);
            contentDefinition = parentValue.getNestedContentDefinition();
        } else {
            // the parent element is the locale element
            parentElement = getLocaleNode(locale);
            elementName = CmsXmlUtils.removeXpathIndex(path);
            contentDefinition = m_contentDefinition;
        }

        // read the XML siblings from the parent node
        List<Element> siblings = CmsXmlGenericWrapper.elements(parentElement, elementName);

        int insertIndex;

        if (contentDefinition.getChoiceMaxOccurs() > 0) {
            // for a choice sequence we do not check the index position, we rather do a full XML validation afterwards

            insertIndex = index;
        } else if (siblings.size() > 0) {
            // we want to add an element to a sequence, and there are elements already of the same type

            if (siblings.size() >= type.getMaxOccurs()) {
                // must not allow adding an element if max occurs would be violated
                throw new CmsRuntimeException(org.opencms.xml.content.Messages.get().container(
                    org.opencms.xml.content.Messages.ERR_XMLCONTENT_ELEM_MAXOCCURS_2,
                    elementName,
                    new Integer(type.getMaxOccurs())));
            }

            if (index > siblings.size()) {
                // index position behind last element of the list
                throw new CmsRuntimeException(org.opencms.xml.content.Messages.get().container(
                    org.opencms.xml.content.Messages.ERR_XMLCONTENT_ADD_ELEM_INVALID_IDX_3,
                    new Integer(index),
                    new Integer(siblings.size())));
            }

            // check for offset required to append beyond last position
            int offset = (index == siblings.size()) ? 1 : 0;
            // get the element from the parent at the selected position
            Element sibling = siblings.get(index - offset);
            // check position of the node in the parent node content
            insertIndex = sibling.getParent().content().indexOf(sibling) + offset;
        } else {
            // we want to add an element to a sequence, but there are no elements of the same type yet

            if (index > 0) {
                // since the element does not occur, index must be 0
                throw new CmsRuntimeException(org.opencms.xml.content.Messages.get().container(
                    org.opencms.xml.content.Messages.ERR_XMLCONTENT_ADD_ELEM_INVALID_IDX_2,
                    new Integer(index),
                    elementName));
            }

            // check where in the type sequence the type should appear
            int typeIndex = contentDefinition.getTypeSequence().indexOf(type);
            if (typeIndex == 0) {
                // this is the first type, so we just add at the very first position
                insertIndex = 0;
            } else {

                // create a list of all element names that should occur before the selected type
                List<String> previousTypeNames = new ArrayList<String>();
                for (int i = 0; i < typeIndex; i++) {
                    I_CmsXmlSchemaType t = contentDefinition.getTypeSequence().get(i);
                    previousTypeNames.add(t.getName());
                }

                // iterate all elements of the parent node
                Iterator<Node> i = CmsXmlGenericWrapper.content(parentElement).iterator();
                int pos = 0;
                while (i.hasNext()) {
                    Node node = i.next();
                    if (node instanceof Element) {
                        if (!previousTypeNames.contains(node.getName())) {
                            // the element name is NOT in the list of names that occurs before the selected type, 
                            // so it must be an element that occurs AFTER the type
                            break;
                        }
                    }
                    pos++;
                }
                insertIndex = pos;
            }
        }

        I_CmsXmlContentValue newValue;
        if (contentDefinition.getChoiceMaxOccurs() > 0) {
            // for a choice we do a full XML validation
            try {
                // append the new element at the calculated position
                newValue = addValue(cms, parentElement, type, locale, insertIndex);
                // validate the XML structure to see if the index position was valid
                // CmsXmlUtils.validateXmlStructure(m_document, m_encoding, new CmsXmlEntityResolver(cms));                
                // can not be validated since recursive schema
            } catch (Exception e) {
                throw new CmsRuntimeException(org.opencms.xml.content.Messages.get().container(
                    org.opencms.xml.content.Messages.ERR_XMLCONTENT_ADD_ELEM_INVALID_IDX_CHOICE_3,
                    new Integer(insertIndex),
                    elementName,
                    parentElement.getUniquePath()));
            }
        } else {
            // just append the new element at the calculated position
            newValue = addValue(cms, parentElement, type, locale, insertIndex);
        }

        // re-initialize this XML content 
        initDocument(m_document, m_encoding, m_contentDefinition);

        // return the value instance that was stored in the bookmarks 
        // just returning "newValue" isn't enough since this instance is NOT stored in the bookmarks
        return getBookmark(getBookmarkName(newValue.getPath(), locale));
    }

    /**
     * Applies the given changes in the current locale, and only in memory.<p>
     * 
     * @param cms the current cms context
     * @param changes the changes to apply
     * @param req the current request
     * 
     * @throws CmsException if something goes wrong
     */
    public void applyChanges(CmsObject cms, List<I_CmsSitemapChange> changes, HttpServletRequest req)
    throws CmsException {

        Locale locale = cms.getRequestContext().getLocale();
        m_newEntryElements.clear();
        // create the locale
        if (!hasLocale(locale)) {
            addLocale(cms, locale);
        }

        Set<String> modified = new HashSet<String>();
        // apply the changes to the raw XML structure
        for (I_CmsSitemapChange change : changes) {
            switch (change.getType()) {
                case DELETE:
                    modified.addAll(deleteEntry(cms, (CmsSitemapChangeDelete)change));
                    break;
                case NEW:
                case SUBSITEMAP_NEW:
                    modified.add(newEntry(cms, (CmsSitemapChangeNew)change, req));
                    break;
                case MOVE:
                    modified.addAll(moveEntry(cms, (CmsSitemapChangeMove)change));
                    break;
                case EDIT:
                    modified.add(editEntry(cms, (CmsSitemapChangeEdit)change));
                    break;
                default:

            }
        }
        createResourcesForNewElements(cms);
        // generate bookmarks
        initDocument(m_document, m_encoding, m_contentDefinition);

        // event handling
        List<String> result = new ArrayList<String>(modified);
        Collections.sort(result);
        OpenCms.fireCmsEvent(new CmsEvent(
            I_CmsEventListener.EVENT_SITEMAP_CHANGED,
            Collections.<String, Object> singletonMap(I_CmsEventListener.KEY_RESOURCES, result)));
    }

    /**
     * Helper method for finding the entry point of the current sitemap.<p>
     * 
     * @param cms the cms context
     * 
     * @return the entry point
     * 
     * @throws CmsException if something goes wrong 
     */
    public String getEntryPoint(CmsObject cms) throws CmsException {

        String entryPoint;
        entryPoint = OpenCms.getSitemapManager().getEntryPoint(cms, cms.getSitePath(getFile()));
        if (entryPoint == null) {
            // if not found, assume absolute paths
            entryPoint = "";
        }
        return entryPoint;
    }

    /**
     * Returns the sitemap bean for the given locale.<p>
     *
     * @param cms the cms context
     * @param locale the locale to use
     *
     * @return the sitemap bean
     */
    public CmsSitemapBean getSitemap(CmsObject cms, Locale locale) {

        Locale theLocale = locale;
        CmsSitemapBean result = m_sitemaps.get(theLocale);
        if (result != null) {
            return result;
        }
        LOG.warn(Messages.get().container(
            Messages.LOG_SITEMAP_LOCALE_NOT_FOUND_2,
            getFile().getRootPath(),
            theLocale.toString()).key());
        theLocale = OpenCms.getLocaleManager().getDefaultLocale(cms, getFile());
        result = m_sitemaps.get(theLocale);
        if (result != null) {
            return result;
        }
        if (getLocales().isEmpty()) {
            // no locale not found!!
            LOG.error(Messages.get().container(
                Messages.LOG_SITEMAP_LOCALE_NOT_FOUND_2,
                getFile().getRootPath(),
                theLocale).key());
            return null;
        }
        // use first locale
        return m_sitemaps.get(getLocales().get(0));
    }

    /**
     * @see org.opencms.xml.content.CmsXmlContent#isAutoCorrectionEnabled()
     */
    @Override
    public boolean isAutoCorrectionEnabled() {

        return true;
    }

    /**
     * @see org.opencms.xml.A_CmsXmlDocument#validateXmlStructure(org.xml.sax.EntityResolver)
     */
    @Override
    public void validateXmlStructure(EntityResolver resolver) {

        // can not be validated since recursive schema
        return;
    }

    /**
     * Creates a new "sitemap entry already exists" exception.<p> 
     * 
     * @param cms the current CMS context
     * @param sitePath the entry's site path
     * 
     * @return a new ready to be thrown exception
     */
    protected CmsSitemapEntryException createEntryAlreadyExistsException(CmsObject cms, String sitePath) {

        String sitemapPath = (getFile() == null) ? null : cms.getSitePath(getFile());
        return new CmsSitemapEntryException(sitemapPath != null ? Messages.get().container(
            Messages.ERR_SITEMAP_ELEMENT_ALREADY_EXISTS_2,
            sitePath,
            sitemapPath) : Messages.get().container(Messages.ERR_SITEMAP_ELEMENT_ALREADY_EXISTS_1, sitePath), null);
    }

    /**
     * Creates a new "sitemap entry not found" exception.<p> 
     * 
     * @param cms the current CMS context
     * @param sitePath the entry's site path
     * 
     * @return a new ready to be thrown exception
     */
    protected CmsSitemapEntryException createEntryNotFoundException(CmsObject cms, String sitePath) {

        String sitemapPath = (getFile() == null) ? null : cms.getSitePath(getFile());
        return new CmsSitemapEntryException(sitemapPath != null ? Messages.get().container(
            Messages.ERR_SITEMAP_ELEMENT_NOT_FOUND_2,
            sitePath,
            sitemapPath) : Messages.get().container(Messages.ERR_SITEMAP_ELEMENT_NOT_FOUND_1, sitePath), null);
    }

    /**
     * Low level deletion of a sitemap entry.<p>
     * 
     * @param cms the CMS context
     * @param change the change to apply
     * 
     * @return the site path of the entries that need to be re-indexed
     * 
     * @throws CmsException if something goes wrong
     */
    protected List<String> deleteEntry(CmsObject cms, CmsSitemapChangeDelete change) throws CmsException {

        Element entryElement = getElement(cms, change.getSitePath());
        if (entryElement == null) {
            // element not found
            throw createEntryNotFoundException(cms, change.getSitePath());
        }

        entryElement.detach();

        // return touched entries
        return getSubTreePaths(
            CmsResource.getParentFolder(cms.getRequestContext().addSiteRoot(change.getSitePath())),
            entryElement);
    }

    /**
     * Low level edition of a sitemap entry.<p>
     * 
     * @param cms the CMS context
     * @param change the change to apply
     * 
     * @return the site path of the entries that need to be re-indexed
     * 
     * @throws CmsException if something goes wrong
     */
    protected String editEntry(CmsObject cms, CmsSitemapChangeEdit change) throws CmsException {

        Element entryElement = getElement(cms, change.getSitePath());
        if (entryElement == null) {
            // element not found
            throw createEntryNotFoundException(cms, change.getSitePath());
        }

        // the title
        if (change.getTitle() != null) {
            Element titleElement = entryElement.element(XmlNode.Title.name());
            if (!titleElement.getText().equals(change.getTitle())) {
                titleElement.clearContent();
                titleElement.addCDATA(change.getTitle());
            }
        }

        // the vfs reference
        if (change.getVfsPath() != null) {
            Element vfsElement = entryElement.element(XmlNode.VfsFile.name());
            fillResource(cms, vfsElement, cms.readResource(change.getVfsPath()));
        }

        // the properties
        if (change.getProperties() != null) {
            Map<String, CmsXmlContentProperty> propertiesConf = OpenCms.getSitemapManager().getElementPropertyConfiguration(
                cms,
                m_file,
                true);

            CmsXmlContentPropertyHelper.saveSimpleProperties(
                cms,
                entryElement,
                change.getProperties(),
                getFile(),
                propertiesConf);
        }

        return cms.getRequestContext().addSiteRoot(change.getSitePath());
    }

    /**
     * Fills a {@link CmsXmlVfsFileValue} with the resource.<p>
     * 
     * @param cms the current CMS context
     * @param element the XML element to fill
     * @param resource the resource to use
     * 
     * @return the resource 
     */
    protected CmsResource fillResource(CmsObject cms, Element element, CmsResource resource) {

        String xpath = element.getPath();
        int pos = xpath.lastIndexOf("/" + XmlNode.SiteEntry.name() + "/");
        if (pos > 0) {
            xpath = xpath.substring(pos + 1);
        }
        CmsRelationType type = getContentDefinition().getContentHandler().getRelationType(xpath);
        CmsXmlVfsFileValue.fillEntry(element, resource.getStructureId(), resource.getRootPath(), type);
        return resource;
    }

    /**
     * Returns the request content value if available, if not a new one will be created.<p>
     * 
     * @param cms the current cms context
     * @param path the value's path
     * @param locale the value's locale
     * @param index the value's index
     * 
     * @return the request content value
     */
    protected I_CmsXmlContentValue getContentValue(CmsObject cms, String path, Locale locale, int index) {

        I_CmsXmlContentValue idValue = getValue(path, locale, index);
        if (idValue == null) {
            idValue = addValue(cms, path, locale, index);
        }
        return idValue;
    }

    /**
     * @see org.opencms.xml.A_CmsXmlDocument#initDocument(org.dom4j.Document, java.lang.String, org.opencms.xml.CmsXmlContentDefinition)
     */
    @Override
    protected void initDocument(Document document, String encoding, CmsXmlContentDefinition definition) {

        m_document = document;
        m_contentDefinition = definition;
        m_encoding = CmsEncoder.lookupEncoding(encoding, encoding);
        m_elementLocales = new HashMap<String, Set<Locale>>();
        m_elementNames = new HashMap<Locale, Set<String>>();
        m_locales = new HashSet<Locale>();
        m_sitemaps = new HashMap<Locale, CmsSitemapBean>();
        clearBookmarks();

        // initialize the bookmarks
        for (Iterator<Element> itSitemaps = CmsXmlGenericWrapper.elementIterator(m_document.getRootElement()); itSitemaps.hasNext();) {
            Element sitemap = itSitemaps.next();

            try {
                Locale locale = CmsLocaleManager.getLocale(sitemap.attribute(
                    CmsXmlContentDefinition.XSD_ATTRIBUTE_VALUE_LANGUAGE).getValue());

                addLocale(locale);

                // get the entries
                List<CmsInternalSitemapEntry> entries = readSubEntries(sitemap, "", definition, locale, "");
                // create the sitemap
                m_sitemaps.put(locale, new CmsSitemapBean(locale, entries));
            } catch (NullPointerException e) {
                LOG.error(org.opencms.xml.content.Messages.get().getBundle().key(
                    org.opencms.xml.content.Messages.LOG_XMLCONTENT_INIT_BOOKMARKS_0), e);
            }
        }
    }

    /**
     * Low level rename/move of a sitemap entry.<p>
     * 
     * @param cms the CMS context
     * @param change the change to apply
     * 
     * @return the site path of the entries that need to be re-indexed
     * 
     * @throws CmsException if something goes wrong
     */
    protected List<String> moveEntry(CmsObject cms, CmsSitemapChangeMove change) throws CmsException {

        Element entryElement = getElement(cms, change.getSourcePath());
        if (entryElement == null) {
            // element not found
            throw createEntryNotFoundException(cms, change.getSourcePath());
        }

        String srcParentPath = CmsResource.getParentFolder(change.getSourcePath());
        String destParentPath = CmsResource.getParentFolder(change.getDestinationPath());
        Element newParent = getElement(cms, destParentPath);
        if (newParent == null) {
            // new parent entry not found
            throw createEntryNotFoundException(cms, destParentPath);
        }

        // move
        if (!srcParentPath.equals(destParentPath)) {
            // detach from old place
            entryElement.detach();
            // at the right position
            List<Element> siblings = CmsCollectionsGenericWrapper.list(newParent.elements(XmlNode.SiteEntry.name()));
            if ((change.getDestinationPosition() < 0) || (change.getDestinationPosition() > siblings.size())) {
                siblings.add(entryElement);
            } else {
                siblings.add(change.getDestinationPosition(), entryElement);
            }

        } else {
            // check position
            List<Element> siblings = CmsCollectionsGenericWrapper.list(newParent.elements(XmlNode.SiteEntry.name()));
            if (siblings.indexOf(entryElement) != change.getDestinationPosition()) {
                siblings.remove(entryElement);
                siblings.add(change.getDestinationPosition(), entryElement);
            }
        }

        // rename
        String srcName = CmsResource.getName(change.getSourcePath());
        String destName = CmsResource.getName(change.getDestinationPath());
        if (!srcName.equals(destName)) {
            Element nameElement = entryElement.element(XmlNode.Name.name());
            nameElement.clearContent();
            if (destName.endsWith("/")) {
                destName = destName.substring(0, destName.length() - 1);
            }
            nameElement.addCDATA(destName);
        }

        // return touched entries
        return getSubTreePaths(cms.getRequestContext().addSiteRoot(destParentPath), entryElement);
    }

    /**
     * Low level insertion of a sitemap entry.<p>
     * 
     * @param cms the CMS context
     * @param change the change to apply
     * @param req the current request
     * 
     * @return the site path of the entries that need to be re-indexed
     * 
     * @throws CmsException if something goes wrong
     */
    protected String newEntry(CmsObject cms, CmsSitemapChangeNew change, HttpServletRequest req) throws CmsException {

        String entryPoint;
        if (change instanceof CmsSitemapChangeNewSubSitemapEntry) {
            entryPoint = ((CmsSitemapChangeNewSubSitemapEntry)change).getEntryPoint();
        } else {
            entryPoint = getEntryPoint(cms);
        }
        Element entryElement = getElement(cms, change.getSitePath(), entryPoint);
        if (entryElement != null) {
            // entry already exists
            throw createEntryAlreadyExistsException(cms, change.getSitePath());
        }

        String parentPath = CmsResource.getParentFolder(change.getSitePath());
        Element parent = getElement(cms, parentPath, entryPoint);
        if (parent == null) {
            // parent entry not found
            throw createEntryNotFoundException(cms, parentPath);
        }
        entryElement = parent.addElement(XmlNode.SiteEntry.name());
        CmsUUID id = change.getId();
        if (id == null) {
            id = new CmsUUID();
        }
        entryElement.addElement(XmlNode.Id.name()).addCDATA(id.toString());
        String name = CmsResource.getName(change.getSitePath());
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }
        entryElement.addElement(XmlNode.Name.name()).addCDATA(name);
        entryElement.addElement(XmlNode.Title.name()).addCDATA(change.getTitle());

        // the vfs reference
        Element vfsFile = entryElement.addElement(XmlNode.VfsFile.name());
        String vfsPath = change.getVfsPath();
        CmsResource resource = null;
        if (vfsPath == null) {
            // We don't create the resource now because subsequent changes may change the entry's name or delete it.
            // Only after all the other changes are processed, we create the resources for new entries.
            m_newEntryElements.add(entryElement);
        } else {
            resource = cms.readResource(vfsPath);
        }
        if (resource != null) {
            fillResource(cms, vfsFile, resource);
        }

        // the properties
        Map<String, CmsXmlContentProperty> propertiesConf = OpenCms.getADEManager().getElementPropertyConfiguration(
            cms,
            getFile());

        CmsXmlContentPropertyHelper.saveSimpleProperties(
            cms,
            entryElement,
            change.getProperties(),
            getFile(),
            propertiesConf);

        return cms.getRequestContext().addSiteRoot(change.getSitePath());
    }

    /**
     * Recursive method to retrieve the sitemap entries with sub entries from the raw XML structure.<p>
     * 
     * @param rootElem the root element
     * @param rootPath the root element path
     * @param rootDef the root content definition
     * @param locale the current locale
     * @param parentUri the parent URI
     * 
     * @return the site entries with sub entries
     */
    protected List<CmsInternalSitemapEntry> readSubEntries(
        Element rootElem,
        String rootPath,
        CmsXmlContentDefinition rootDef,
        Locale locale,
        String parentUri) {

        List<CmsInternalSitemapEntry> entries = new ArrayList<CmsInternalSitemapEntry>();
        for (Iterator<Element> itCnts = CmsXmlGenericWrapper.elementIterator(rootElem, XmlNode.SiteEntry.name()); itCnts.hasNext();) {
            Element entry = itCnts.next();

            // entry itself
            int entryIndex = CmsXmlUtils.getXpathIndexInt(entry.getUniquePath(rootElem));
            String entryPath = CmsXmlUtils.createXpathElement(entry.getName(), entryIndex);
            if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(rootPath)) {
                entryPath = CmsXmlUtils.concatXpath(rootPath, entryPath);
            }
            I_CmsXmlSchemaType entrySchemaType = rootDef.getSchemaType(entry.getName());
            I_CmsXmlContentValue entryValue = entrySchemaType.createValue(this, entry, locale);
            addBookmark(entryPath, locale, true, entryValue);
            CmsXmlContentDefinition entryDef = ((CmsXmlNestedContentDefinition)entrySchemaType).getNestedContentDefinition();

            // id
            Element id = entry.element(XmlNode.Id.name());
            if (id == null) {
                // create element if missing
                id = entry.addElement(XmlNode.Id.name());
            }
            if (CmsStringUtil.isEmptyOrWhitespaceOnly(id.getTextTrim())) {
                // create a new id if missing
                id.addCDATA(new CmsUUID().toString());
            }
            if (!CmsUUID.isValidUUID(id.getTextTrim())) {
                // create a new valid id if it is not valid
                id.clearContent();
                id.addCDATA(new CmsUUID().toString());
            }
            addBookmarkForElement(id, locale, entry, entryPath, entryDef);
            CmsUUID entryId = new CmsUUID(id.getTextTrim());

            // name
            Element name = entry.element(XmlNode.Name.name());
            addBookmarkForElement(name, locale, entry, entryPath, entryDef);
            String entryName = name.getTextTrim();

            // title
            Element title = entry.element(XmlNode.Title.name());
            addBookmarkForElement(title, locale, entry, entryPath, entryDef);
            String titleValue = title.getTextTrim();

            // vfs file
            Element uri = entry.element(XmlNode.VfsFile.name());
            addBookmarkForElement(uri, locale, entry, entryPath, entryDef);
            Element linkUri = uri.element(CmsXmlPage.NODE_LINK);
            CmsUUID uriId = null;
            if (linkUri == null) {
                // this can happen when adding the entry node to the xml content
                // it is not dangerous since the link has to be set before saving 
            } else {
                uriId = new CmsLink(linkUri).getStructureId();
            }

            // properties
            Map<String, CmsSimplePropertyValue> ownProps = CmsXmlContentPropertyHelper.readSimpleProperties(
                this,
                locale,
                entry,
                entryPath,
                entryDef);

            String path = parentUri + entryName;
            if (parentUri.equals("/")) {
                path = entryName;
            }
            if (!path.endsWith("/")) {
                path += "/";
            }
            List<CmsInternalSitemapEntry> subEntries = readSubEntries(entry, entryPath, entryDef, locale, path);

            entries.add(new CmsInternalSitemapEntry(
                entryId,
                path,
                uriId,
                entryName,
                titleValue,
                false,
                ownProps,
                subEntries,
                null));
        }
        return entries;
    }

    /**
     * @see org.opencms.xml.content.CmsXmlContent#setFile(org.opencms.file.CmsFile)
     */
    @Override
    protected void setFile(CmsFile file) {

        // just for visibility from the factory
        super.setFile(file);
    }

    /**
     * Creates the resource for an element corresponding to a new sitemap entry.<p>
     * 
     * @param cms the current CMS context 
     * @param elem the element corresponding to the new sitemap entry 
     * 
     * @return the newly created resource
     * 
     * @throws CmsException if something goes wrong 
     */
    private CmsResource createResourceForNewElement(CmsObject cms, Element elem) throws CmsException {

        CmsResource resource;
        String sitePath = getSitePathForElement(cms, elem);
        String title = elem.element(XmlNode.Title.name()).getText();

        String sitemapUri = m_file.getRootPath();
        resource = OpenCms.getSitemapManager().createPage(
            cms,
            cms.getRequestContext().removeSiteRoot(sitemapUri),
            title,
            sitePath);

        setResourceTitle(cms, resource, title);
        return resource;
    }

    /**
     * Creates the resources for all elements corresponding to new sitemap entries.<p>
     * 
     * @param cms the current CMS context 
     * 
     * @throws CmsException if something goes wrong 
     */
    private void createResourcesForNewElements(CmsObject cms) throws CmsException {

        for (Element elem : m_newEntryElements) {
            // we can't directly check the parent of the element, because the element
            // may be deep in a whole subtree which has been deleted. 
            List<Element> ancestors = getSitemapEntryAncestorElements(elem);
            Element first = ancestors.get(0);
            if (first.getParent() == null) {
                // entry was deleted, so we ignore the element  
                continue;
            }

            CmsResource resource = createResourceForNewElement(cms, elem);
            Element vfsFile = elem.element(XmlNode.VfsFile.name());
            fillResource(cms, vfsFile, resource);
        }
    }

    /**
     * Locates a DOM element for the given sitemap path.<p>
     * 
     * 
     * @param cms the CMS context 
     * @param sitePath the site path
     * 
     * @return the corresponding DOM element or <code>null</code> if not found
     * 
     * @throws CmsException if something goes wrong 
     */
    private Element getElement(CmsObject cms, String sitePath) throws CmsException {

        return getElement(cms, sitePath, getEntryPoint(cms));
    }

    /**
     * Locates a DOM element for the given sitemap path.<p>
     * 
     * @param cms the CMS context 
     * @param sitePath the site path
     * @param entryPoint the entryPoint to use
     * 
     * @return the corresponding DOM element or <code>null</code> if not found
     * 
     */
    private Element getElement(CmsObject cms, String sitePath, String entryPoint) {

        Element parent = getLocaleNode(cms.getRequestContext().getLocale());
        String originalUri = sitePath.substring(entryPoint.length());
        String[] pathEntries = CmsStringUtil.splitAsArray(originalUri, '/');

        // handle special case of root node in root sitemap
        if (parent.elements(XmlNode.SiteEntry.name()).size() == 1) {
            Element element = parent.element(XmlNode.SiteEntry.name());
            if (CmsStringUtil.isEmptyOrWhitespaceOnly(element.elementText(XmlNode.Name.name()))) {
                parent = element;
            }
        }

        // tree level iteration
        for (String name : pathEntries) {
            boolean found = false;
            // level entry iteration
            for (Element element : CmsCollectionsGenericWrapper.<Element> list(parent.elements(XmlNode.SiteEntry.name()))) {
                if (element.elementText(XmlNode.Name.name()).equals(name)) {
                    found = true;
                    parent = element;
                    break;
                }
            }
            if (!found) {
                return null;
            }
        }
        return parent;
    }

    /**
     * Given an XML element corresponding to a sitemap entry, this method returns a list of XML
     * elements corresponding to the ancestor sitemap entries of the entry.<p>
     * 
     * The list starts with the topmost ancestor and ends with the element which was passed in.<p>
     * 
     * @param elem the element for which the ancestor elements should be retrieved 
     * 
     * @return the list of ancestor elements 
     */
    private List<Element> getSitemapEntryAncestorElements(Element elem) {

        assert elem.getName().equals(XmlNode.SiteEntry.name());
        LinkedList<Element> ancestors = new LinkedList<Element>();
        Element currentElement = elem;
        while (currentElement.getName().equals(XmlNode.SiteEntry.name())) {
            // adding the ancestors in reverse order 
            ancestors.addFirst(currentElement);
            currentElement = currentElement.getParent();
            if (currentElement == null) {
                break;
            }
        }
        return ancestors;
    }

    /**
     * Returns the sitemap path for a given XML element corresponding to a sitemap entry.<p>
     * 
     * @param cms the current CMS context 
     * @param elem the element for which the sitemap path should be retrieved
     *  
     * @return the sitemap path for the element 
     * 
     * @throws CmsException if something goes wrong 
     */
    private String getSitePathForElement(CmsObject cms, Element elem) throws CmsException {

        assert elem.getName().equals(XmlNode.SiteEntry.name());
        String result = "";
        List<Element> ancestors = getSitemapEntryAncestorElements(elem);
        String entryPoint = getEntryPoint(cms);
        List<String> pathComponents = new ArrayList<String>();
        pathComponents.add(entryPoint);
        for (Element ancestor : ancestors) {
            Element nameNode = ancestor.element(XmlNode.Name.name());
            String name = nameNode.getText();
            pathComponents.add(name);
        }
        result = CmsStringUtil.joinPaths(pathComponents);
        return result;
    }

    /**
     * Returns the list of all paths in the element tree, being the given element child of the given parent path.<p>
     * 
     * @param parentPath the parent path to start with
     * @param entryElement the element to start with
     * 
     * @return the list of paths
     */
    private List<String> getSubTreePaths(String parentPath, Element entryElement) {

        String srcPath = parentPath;
        List<String> result = new ArrayList<String>();
        if (srcPath.endsWith("/")) {
            srcPath = srcPath.substring(0, srcPath.length() - 1);
        }
        Map<String, List<Element>> subEntries = new HashMap<String, List<Element>>();
        subEntries.put(srcPath, Collections.singletonList(entryElement));
        while (!subEntries.isEmpty()) {
            String path = subEntries.keySet().iterator().next();
            List<Element> entries = subEntries.remove(path);
            for (Element entry : entries) {
                String entryPath = path + "/" + entry.elementText(XmlNode.Name.name());
                if (entryPath.endsWith("/")) {
                    entryPath = entryPath.substring(0, entryPath.length() - 1);
                }
                result.add(entryPath + "/");
                // continue recursion
                List<Element> children = CmsCollectionsGenericWrapper.<Element> list(entry.elements(XmlNode.SiteEntry.name()));
                if (!children.isEmpty()) {
                    subEntries.put(entryPath, children);
                }
            }
        }
        return result;
    }

    /**
     * Sets the title property of a given resource.<p>
     * 
     * @param cms the CMS context
     * @param resource the resource
     * @param title the title
     * @throws CmsException if something goes wrong
     */
    private void setResourceTitle(CmsObject cms, CmsResource resource, String title) throws CmsException {

        CmsProperty currentProperty = cms.readPropertyObject(resource, CmsPropertyDefinition.PROPERTY_TITLE, false);
        // detect if property is a null property or not
        if (currentProperty.isNullProperty()) {
            // create new property object and set key and value
            currentProperty = new CmsProperty();
            currentProperty.setName(CmsPropertyDefinition.PROPERTY_TITLE);
            if (OpenCms.getWorkplaceManager().isDefaultPropertiesOnStructure()) {
                // set structure value
                currentProperty.setStructureValue(title);
                currentProperty.setResourceValue(null);
            } else {
                // set resource value
                currentProperty.setStructureValue(null);
                currentProperty.setResourceValue(title);
            }
        } else if (currentProperty.getStructureValue() != null) {
            // structure value has to be updated
            currentProperty.setStructureValue(title);
            currentProperty.setResourceValue(null);
        } else {
            // resource value has to be updated
            currentProperty.setStructureValue(null);
            currentProperty.setResourceValue(title);
        }
        CmsLock lock = cms.getLock(resource);
        if (lock.isUnlocked()) {
            // lock resource before operation
            cms.lockResource(cms.getSitePath(resource));
        }
        // write the property to the resource
        cms.writePropertyObject(cms.getSitePath(resource), currentProperty);
        // unlock the resource
        cms.unlockResource(cms.getSitePath(resource));
    }
}