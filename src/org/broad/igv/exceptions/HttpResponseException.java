/*
 * Copyright (c) 2007-2011 by The Broad Institute of MIT and Harvard.  All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR
 * WARRANTES OF ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING,
 * WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER
 * OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR RESPECTIVE
 * TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES
 * OF ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES,
 * ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER
 * THE BROAD OR MIT SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT
 * SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
 */

package org.broad.igv.exceptions;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Class for converting HTTP error response codes to exceptions
 *
 * @author Jim Robinson
 * @date Jul 27, 2011
 */
public class HttpResponseException extends IOException {

    int statusCode;

    public HttpResponseException(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String getMessage() {
        
        String scode = " (status code " + statusCode + ")";
        String prefs = "<br>(check menu <b>View/Preferences/Proxy settings</b> (even if you use no proxy) to set default username/pw)" ;
        String user = " (maybe try a different username/password) ";
        switch (statusCode) {
            case 407:
                return "<br>Proxy authentication required"+user+scode+prefs;
            case 403:
                return "<br>Access Forbidden"+user+scode+prefs;
            case 404:
                return "<br>File (<b>or its index file</b>) was not found"+scode;
            case 401:
                return "<br>Access Forbidden "+user+scode+prefs;
            case 500:
                return "<br>The file (<b>or its index file</b>) was not found "+scode;
            case 503:
                return "<br>The server is not available: "+scode+"<br>Maybe it is overloaded or being restarted?";
            default:
                return "<br>HTTP access error"+user+scode+prefs;
        }

    }
}
