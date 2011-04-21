/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/jsp/CmsJspTagFormatter.java,v $
 * Date   : $Date: 2011/04/20 07:07:49 $
 * Version: $Revision: 1.2 $
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

package org.opencms.jsp;

import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.flex.CmsFlexController;
import org.opencms.i18n.CmsLocaleManager;
import org.opencms.jsp.util.CmsJspContentAccessBean;
import org.opencms.main.CmsException;
import org.opencms.main.CmsIllegalArgumentException;
import org.opencms.main.OpenCms;
import org.opencms.util.CmsStringUtil;
import org.opencms.xml.containerpage.CmsContainerElementBean;

import java.util.Locale;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

/**
 * Implementation of the <code>&lt;cms:formatter var="..." val="..." /&gt;</code> tag, 
 * used to access and display XML content item information in a formatter.<p>
 * 
 * @author Andreas Zahner 
 * 
 * @version $Revision: 1.2 $ 
 * 
 * @since 8.0.0 
 */
public class CmsJspTagFormatter extends CmsJspScopedVarBodyTagSuport {

    /** Serial version UID required for safe serialization. */
    private static final long serialVersionUID = -8232834808735187624L;

    /** The CmsObject for the current user. */
    protected transient CmsObject m_cms;

    /** The FlexController for the current request. */
    protected CmsFlexController m_controller;

    /** Reference to the last loaded resource element. */
    protected transient CmsResource m_resource;

    /** The current container element. */
    private CmsContainerElementBean m_element;

    /** Reference to the currently selected locale. */
    private Locale m_locale;

    /** Optional name for the attribute that provides direct access to the content value map. */
    private String m_value;

    /**
     * Empty constructor, required for JSP tags.<p> 
     */
    public CmsJspTagFormatter() {

        super();
    }

    /**
     * Constructor used when using <code>formatter</code> from scriptlet code.<p> 
     * 
     * @param context the JSP page context
     * @param locale the locale to use 
     * 
     * @throws JspException in case something goes wrong
     */
    public CmsJspTagFormatter(PageContext context, Locale locale)
    throws JspException {

        m_locale = locale;
        setPageContext(context);
        init();
    }

    /**
     * @see javax.servlet.jsp.tagext.Tag#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException, CmsIllegalArgumentException {

        // initialize the content load tag
        init();
        return EVAL_BODY_INCLUDE;
    }

    /**
     * Returns the locale.<p>
     *
     * @return the locale
     */
    public String getLocale() {

        return (m_locale != null) ? m_locale.toString() : "";
    }

    /**
     * Returns the name for the optional attribute that provides direct access to the content value map.<p>
     * 
     * @return the name for the optional attribute that provides direct access to the content value map
     */
    public String getVal() {

        return m_value;
    }

    /**
     * @see javax.servlet.jsp.tagext.Tag#release()
     */
    @Override
    public void release() {

        m_locale = null;
        m_cms = null;
        m_resource = null;
        m_controller = null;
        super.release();
    }

    /**
     * Sets the locale.<p>
     *
     * @param locale the locale to set
     */
    public void setLocale(String locale) {

        if (CmsStringUtil.isEmpty(locale)) {
            m_locale = null;
        } else {
            m_locale = CmsLocaleManager.getLocale(locale);
        }
    }

    /**
     * Sets the name for the optional attribute that provides direct access to the content value map.<p>
     * 
     * @param val the name for the optional attribute that provides direct access to the content value map
     */
    public void setVal(String val) {

        if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(val)) {
            m_value = val.trim();
        }
    }

    /**
     * Initializes this formatter tag.<p> 
     * 
     * @throws JspException in case something goes wrong
     */
    protected void init() throws JspException {

        // initialize OpenCms access objects
        m_controller = CmsFlexController.getController(pageContext.getRequest());
        m_cms = m_controller.getCmsObject();

        try {
            // get the resource name from the selected container
            m_element = OpenCms.getADEManager().getCurrentElement(pageContext.getRequest());
            m_element.initResource(m_cms);
            if (m_locale == null) {
                // no locale set, use locale from users request context
                m_locale = m_cms.getRequestContext().getLocale();
            }

            // load content and store it
            CmsJspContentAccessBean bean = new CmsJspContentAccessBean(m_cms, m_locale, m_element.getResource());
            storeAttribute(getVar(), bean);

            if (m_value != null) {
                // if the optional "val" parameter has been set, store the value map of the content in the page context scope
                storeAttribute(getVal(), bean.getValue());
            }

        } catch (CmsException e) {
            m_controller.setThrowable(e, m_cms.getRequestContext().getUri());
            throw new JspException(e);
        }
    }
}