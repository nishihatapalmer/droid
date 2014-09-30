/**
 * Copyright (c) 2012, The National Archives <pronom@nationalarchives.gsi.gov.uk>
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
package uk.gov.nationalarchives.droid.profile.export;

import org.hibernate.SessionFactory;
//import org.hibernate.ejb.HibernateEntityManagerFactory;
//BNO: replaced namespace above with this one as we were getting a spring type mismatch
//This is now the recommended approach as the ejb version is deprecated - see:
//http://docs.jboss.org/hibernate/orm/4.3/javadocs/org/hibernate/ejb/HibernateEntityManagerFactory.html
import org.hibernate.jpa.HibernateEntityManagerFactory;

/**
 * @author rflitcroft
 *
 */
public final class HibernateSessionFactoryLocator {

    private HibernateSessionFactoryLocator() { }
    
    //private HibernateEntityManagerFactory entityManagerFactory;
    
    /**
     * Gets the hibernate session factory from the entitymanager factory.
     * @param emf an entity manager factory (must a hibernate one!)
     * @return hibernate session factory
     */
    public static SessionFactory getSessionFactory(HibernateEntityManagerFactory emf) {
        return emf.getSessionFactory();
    }

//    /**
//     * @param entityManagerFactory the entityManagerFactory to set
//     */
//    public void setEntityManagerFactory(
//            HibernateEntityManagerFactory entityManagerFactory) {
//        this.entityManagerFactory = entityManagerFactory;
//    }
//    
//    /**
//     * @return the hibernate session factory
//     */
//    public SessionFactory getSessionFactory() {
//        return entityManagerFactory.getSessionFactory();
//    }
}