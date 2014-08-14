/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.karyo.views;

import com.iontorrent.karyo.data.KaryoTrack;
import com.iontorrent.karyo.renderer.RenderManager;
import com.iontorrent.prefs.IonTorrentPreferencesManager;
import com.iontorrent.utils.ErrorHandler;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.apache.log4j.Logger;
import org.broad.igv.PreferenceManager;
import org.broad.igv.sam.CoverageTrack;
import org.broad.igv.track.AbstractTrack;
import org.broad.igv.track.DataSourceTrack;
import org.broad.igv.track.FeatureTrack;
import org.broad.igv.track.Track;
import org.broad.igv.track.TrackType;
import org.broad.igv.ui.IGV;

/**
 *
 * @author Chantal Roth
 */
public class IgvTrackSelectionPanel extends javax.swing.JPanel {

    private int nrlisted;
    private ArrayList<KaryoTrack> karyotracks;
    boolean toggleall = true;
    private ArrayList<SingleTrackPanel> panels;
    
    /**
     * Creates new form TrackSelectionPanel
     */
    public IgvTrackSelectionPanel(KaryoControlPanel control) {
        initComponents();
        panels = new ArrayList<SingleTrackPanel> ();
        setLayout(new BorderLayout());
        JPanel north = new JPanel();
        north.setLayout(new BorderLayout());
        north.add("North",new JLabel("Please pick the tracks you wish to see in the Karyo View") );
        JButton btnall = new JButton("Toggle all");
        north.add("West", btnall);
        btnall.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                for (SingleTrackPanel cb: panels) {
                    cb.setSelected(!cb.isSelected());
                }
                repaint();
                toggleall = !toggleall;
            }
            
        });
        north.add("North",new JLabel("Please pick the tracks you wish to see in the Karyo View") );
        add("North", north);
        
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        Collection<Track> tracks = IGV.getInstance().getAllTracks();

        // karyotracks = control.getManager().getKaryoTracks();
        if (karyotracks == null) {
            karyotracks = new ArrayList<KaryoTrack>();
        }

        MouseListener list = null;
        int count = 0;
        p("Found "+tracks.size()+" IGV tracks");
        boolean selectedByDefault = tracks.size() < 5;
        for (Track igvtrack : tracks) {

            p("Got track: " + igvtrack.getDisplayName() + ", " + igvtrack.getClass().getName());
            TrackType type = igvtrack.getTrackType();
            String n = igvtrack.getDisplayName();
            n = n.replace("<br>", " ");
            //  p("Got track " + n + ":" + type);
            PreferenceManager prefs = PreferenceManager.getInstance();
            
            boolean allowbam = prefs.getAsBoolean(IonTorrentPreferencesManager.KARYO_ALLOW_BAMFILES);
            boolean allowgene = prefs.getAsBoolean(IonTorrentPreferencesManager.KARYO_ALLOW_GENEFILES);
            boolean allowexp = prefs.getAsBoolean(IonTorrentPreferencesManager.KARYO_ALLOW_EXPFILES);
            try {
                if (!(igvtrack instanceof AbstractTrack)) {
                    p("Don't know what to do with this track type " + n + "/" + type);
                } else {
                    AbstractTrack atrack = (AbstractTrack) igvtrack;
                    //   p(n +"/"+type+"/"+atrack.getUrl()+" is an abstract track: "+atrack.getClass().getName());
                    count++;
                    KaryoTrack ktrack = new KaryoTrack(atrack, count);
                    String file = n;
                    if (ktrack.getTrack().getResourceLocator() != null) {
                        file = ktrack.getTrack().getResourceLocator().getFileName();
                    }
                   boolean visible = true;
                    p("================= got track name "+ktrack.getTrackDisplayName()+" -> "+ktrack.getRenderType().getKaryoDisplayName());
                    String trackname = ktrack.getRenderType().getKaryoDisplayName();
                    if (trackname != null) {
                        // also get sample
                        String sample = ktrack.getSample();
                        
                        if (sample != null) {
                            sample = Character.toUpperCase(sample.charAt(0)) + sample.substring(1);
                            trackname = sample+" "+trackname;
                        }
                        ktrack.setTrackDisplayName(trackname);
                    }
                    String displayname =ktrack.getTrackDisplayName();
                    p("Got displyaname: "+displayname);
                    SingleTrackPanel cb = null;
                    if (atrack instanceof DataSourceTrack) {
                        p("It is a datasource track");
                        if ((n.endsWith(".bam") || n.endsWith(".BAM") || file.endsWith(".bam") || file.endsWith(".BAM"))) {
                            p(n + "/" + type + ": Got a file with .bam" + n);
                            // check preferences if we want to use this or not
                            if (allowbam) {
                                cb = new SingleTrackPanel(ktrack, true, control, list);
                                cb.setSelected(false);
                            }
                            else visible = false;
                        } else {
                            cb = new SingleTrackPanel(ktrack, true, control, list);
                            if (n.toLowerCase().contains("ploidy")) {
                                p("NOT allowing ploidy .seg tracks in karyo view)");
                                visible = false;
                            }
                            else {
                                cb.setSelected(selectedByDefault);
                                p(n + "/" + type + " : Adding DataSourceTrack");
                            }
                            
                        }
                    } else if (atrack instanceof CoverageTrack) {
                        p("It is a CoverageTrack");

                        if (n.endsWith(".bam") || n.endsWith(".BAM") || file.endsWith(".bam") || file.endsWith(".BAM")) {
                            p("Got a file with .bam:" + n);
                            if (allowbam) {
                                cb = new SingleTrackPanel(ktrack, true, control, list);
                                cb.setSelected(false);
                            }
                            // check preferences if we want to use this or not
                            else visible = false;
                        } else {
                            cb = new SingleTrackPanel(ktrack, true, control, list);
                            p("Adding coverage track");
                        }

                    } else if (atrack instanceof FeatureTrack) {
                        p("displayname="+ displayname+", n="+n + "/" + type + "/" + file + " is a feature track");
                        String disp = displayname.toLowerCase();
                        if (n.startsWith("RefSeq") || n.indexOf("Ensemble") > -1) {
                            // ignore
                            p("Ignoring RefSeq and Ensemble genes");
                        }
                        else if (!allowbam && (disp.startsWith("mother") || disp.startsWith("father"))) {
                         //   p("Ignoring mother and father for now as it was not tested and take too long!"); 
                              cb = new SingleTrackPanel(ktrack, true, control, list);
                              cb.setToolTipText("This type could potentially take <b>long to load</b> and use <b>a lot of memory</b>");
                              cb.setSelected(false);
                        } else {

                            if (type == TrackType.GENE || type == TrackType.CHIP || type == TrackType.EXPR) {

                                if ((allowgene && type == TrackType.GENE) || allowexp) {
                                    cb = new SingleTrackPanel(ktrack, true, control, list);
                                    cb.setToolTipText("This type could potentially take long to load");
                                    p("This track could take too long, not adding");
                                } else {
                                    visible = false;
                                }

                            } else if (n.endsWith(".bam") || n.endsWith(".BAM") || file.endsWith(".bam") || file.endsWith(".BAM")) {
                                p("Got a file with .bam:" + n);
                                // check preferences if we want to use this or not

                                if (allowbam) {
                                    cb = new SingleTrackPanel(ktrack, true, control, list);
                                    cb.setSelected(false);
                                    p("This is a bam file");
                                    if (!atrack.getResourceLocator().isLocal()) {
                                        cb.setText("Bam files can be huge, and this one is remote. It could take too long to load the entire file remotely!"
                                                + "<br>You can store it locally first and then load it if you really want to.");
                                    } else {
                                        cb.setText("Bam files can be huge, and it could take too long to load the entire file!");
                                    }
                                }
                                else  visible = false;
                            } else if (n.endsWith(".txt.gz") ||  file.endsWith(".txt.gz") ) {
                                p("Got a file with .txt.gz:" + n);

                                if (allowgene) {
                                    cb = new SingleTrackPanel(ktrack, true, control, list);
                                    cb.setSelected(false);
                                    p("This is a gene file");
                                    if (!atrack.getResourceLocator().isLocal()) {
                                        cb.setText("Gene files can be large, and this one is remote. It could take some time to load the entire file remotely!"
                                                + "<br>You can store it locally first and then load it if you really want to.");                                    
                                    }
                                }
                                else  visible = false;
                            } else {
                             // what about large .bed files?
                               
                                cb = new SingleTrackPanel(ktrack, true, control, list);
                                if (file.endsWith(".vcf") || file.endsWith(".vcf.gz")) {
                                    if (n.toLowerCase().contains("ploidy")) cb.setSelected(true);
                                    else cb.setSelected(selectedByDefault);
                                }
                                else cb.setSelected(selectedByDefault);
                            }

                        }
                    }
                    if (cb != null) {
                        GuiProperties gui = RenderManager.getGuiProperties();
                       // boolean visible = gui.isKaryoVisible(ktrack.getGuiSample(), ktrack.getGuiKey(), ktrack.getFileExt());
                        if ( !visible)  {
                            p("NOT adding "+n+", ktrack.isvisible: "+ktrack.isVisible());
                            
                        }
                        else {                           
                            center.add(cb);
                            panels.add(cb);
                            karyotracks.add(ktrack);
                            nrlisted++;
                        }
                    }
                    else p("Got no panel for "+n);
                }

            } catch (Exception e) {
                p(ErrorHandler.getString(e));
            }
        }
        //if ()
        this.add("Center", new JScrollPane(center));
        
        
    }

    public int getNrListedTracks() {
        return nrlisted;
    }

    public int getNrTracks() {
        return karyotracks.size();
    }

    private void p(String msg) {
        System.out.println("IgvTrackSelectionPanel: " + msg);
        Logger.getLogger("IgvTrackSelectionPanel").info(msg);
    }

    public ArrayList<KaryoTrack> getSelectedTracks() {
        ArrayList<KaryoTrack> visible = new ArrayList<KaryoTrack>();
        for (KaryoTrack kt : karyotracks) {
            if (kt.isVisible())visible.add(kt);
        }
        return visible;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
