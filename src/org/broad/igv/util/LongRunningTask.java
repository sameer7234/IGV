/*
 * Copyright (c) 2007-2012 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 */
package org.broad.igv.util;

import com.iontorrent.utils.ErrorHandler;
import org.apache.log4j.Logger;
import org.broad.igv.Globals;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.WaitCursorManager;
import org.broad.igv.ui.WaitCursorManager.CursorToken;
import org.broad.igv.ui.util.MessageUtils;

import javax.swing.*;
import java.util.concurrent.*;

/**
 * Utility class for executing long running tasks in their own thread (i.e. not on the swing event thread).
 *
 * @author jrobinso
 */
public class LongRunningTask implements Callable {

    private static Logger log = Logger.getLogger(LongRunningTask.class);

    private static ExecutorService threadExecutor = Executors.newFixedThreadPool(15);
    private static ScheduledExecutorService schedule = Executors.newScheduledThreadPool(4);

    Runnable runnable;

    public static Future submit(Runnable runnable) {
        if (Globals.isBatch() || !SwingUtilities.isEventDispatchThread()) {
            runnable.run();
            return null;
        } else {            
            return threadExecutor.submit(new LongRunningTask(runnable));
        }
    }

    public LongRunningTask(Runnable runnable) {
        this.runnable = runnable;
    }

    public Object call() throws Exception {

        CursorToken token = WaitCursorManager.showWaitCursor();
        try {
            
            runnable.run();
        } catch (Exception e) {
            String err = ErrorHandler.getString(e);
            err = err.replaceAll("\n", "\n<br>");
            MessageUtils.showMessage("Unexpected error: " + e.getMessage() + "\n<br>"+err+"\n<br>See igv.log in your users folder/igv for more details");
            log.error("Exception running task: "+ErrorHandler.getString(e), e);
        } finally {
            //log.info("Removing wait cursor " + runnable.getName());
            WaitCursorManager.removeWaitCursor(token);
            IGV.getInstance().resetStatusMessage();
            synchronized (IGV.getInstance()) {
                IGV.getInstance().notifyAll();
            }
        }
        return null;

    }


}

