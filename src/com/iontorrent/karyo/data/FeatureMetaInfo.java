/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.karyo.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import org.broad.igv.data.seg.Segment;
import org.broad.igv.feature.BasicFeature;
import org.broad.igv.feature.LocusScore;
import org.broad.igv.variant.Variant;
import org.broad.tribble.Feature;

/**
 * information about fields and values
 *
 * @author Chantal Roth
 */
public abstract class FeatureMetaInfo {

    public static int MAX_NR_FEATURES = 200000;
    protected ArrayList<String> atts;
    protected HashMap<String, ArrayList<String>> attvaluesmap;
    protected HashMap<String, Range> attrangemap;
    private int msgs;
    private String trackname;
    
    
    public FeatureMetaInfo(String trackname) {
        atts = new ArrayList<String>();
        this.trackname = trackname;
        attvaluesmap = new HashMap<String, ArrayList<String>>();
        attrangemap = new HashMap<String, Range>();
    }
     public String getScoreFieldName(Feature f) {
        return "Score";
    }

    public double getValue(String relevantAttName, Feature f) {
        p("getValue(String relevantAttName, Feature f): not implemented");
        return 0;
    }

    /**
     * @return the trackname
     */
    public String getTrackname() {
        return trackname;
    }

    /**
     * @param trackname the trackname to set
     */
    public void setTrackname(String trackname) {
        this.trackname = trackname;
    }

    public  class Range {

        public double min;
        public double max;
        int nr;

        public Range() {
            min = Integer.MAX_VALUE;
            max = Integer.MIN_VALUE;
        }

        public void add(double d) {
            if (d > max) {
             //   p("Range.add: New max for "+trackname+":"+d);
                max = d;
            }
            if (d < min) {
                min = d;
              //  p("Range: New min for "+trackname+":"+d);
            }
            nr++;
        }

        public double getMiddle() {
            return (max - min) / 2;
        }
        public String toString() {
            return min+"-"+max;
        }
    }

    
    public static FeatureMetaInfo createMetaInfo(KaryoTrack track, Feature samplefeature) {
     //   p("Creating metainfo for feature " + samplefeature.getClass().getName());
        if (samplefeature instanceof Variant) {
    //        p("Creating variant meta info");
            return new VariantMetaInfo(track.getTrackDisplayName());
        } else if (samplefeature instanceof Segment) {
            Segment f = (Segment) samplefeature;            
   //         p("Got type:" + f.getDescription() + ", creating SegmentMetaInfo");
            return new SegmentMetaInfo(track.getTrackDisplayName());
        } else if (samplefeature instanceof LocusScore) {
            LocusScore f = (LocusScore) samplefeature;
  //          p("Got type:" + f+ ", creating LocusScoreMetaInfo");
            return new LocusScoreMetaInfo(track.getTrackDisplayName());
        } else if (samplefeature instanceof BasicFeature) {

            BasicFeature f = (BasicFeature) samplefeature;
   //         p("Got type:" + f.getType());
            return new BedMetaInfo(track.getTrackDisplayName());
        } else {
            err("Don't know what meta info to use for " + samplefeature.getClass().getName());
            return null;
        }
    }

    public abstract void populateMetaInfo(Feature f);

    public void addAtt(String att) {
        if (!atts.contains(att)) {
            atts.add(att);
        }
    }

    public void addAtt(String att, double value) {
        Range r = attrangemap.get(att.toUpperCase());
        if (r == null) {
            r = new Range();
            attrangemap.put(att.toUpperCase(), r);
        }
        if (value < r.min || value > r.max) {
           // p("adding range "+value+ " for "+att);
            msgs++;
        }
        r.add(value);
        addAtt(att);
    }

    public void addAtt(String att, String value) {
        // check fo rnumber
        try {
            Double d = Double.parseDouble(value);
            addAtt(att, d.doubleValue());
            return;
        } catch (Exception e) {
        }
        ArrayList<String> vals = attvaluesmap.get(att.toUpperCase());
        if (vals == null) {
            vals = new ArrayList<String>();
            attvaluesmap.put(att.toUpperCase(), vals);
        }
        if (!vals.contains(value)) {
            vals.add(value);
        }
    }

    public void populateMetaInfo(FeatureTree tree) {
        int nrprocessed = 0;
        // we collect data for say at least 1000 features
        for (int b = 0; b < tree.getNrbuckets(); b++) {
            FeatureTreeNode node = tree.getNodeForBucket(b);
            if (node != null && node.getTotalNrChildren() > 0) {
                for (Feature f : node.getAllFeatures()) {
                    if (f != null) {
                        if (f instanceof KaryoFeature) {
                            populateMetaInfo(((KaryoFeature)f).getFeature());
                        }
                        else populateMetaInfo(f);
                        nrprocessed++;
                        if (nrprocessed > MAX_NR_FEATURES) {
                            return;
                        }

                    }
                };

            }
        }
    }

    public ArrayList<String> getAttributes() {
        return atts;
    }

    public boolean isRange(String name) {
        return attrangemap.keySet().contains(name.toUpperCase());
    }

    public ArrayList<String> getValuesForAttribute(String att) {
        return attvaluesmap.get(att.toUpperCase());
    }

    public Range getRangeForAttribute(String att) {
        return attrangemap.get(att.toUpperCase());
    }

    public void showRanges() {
        p("Got ranges: "+attrangemap.keySet()+", "+attrangemap.size());
    }
    private static void p(String msg) {
        Logger.getLogger("FeatureMetaInfo").info(msg);
    }

    private static void err(String msg) {
        Logger.getLogger("FeatureMetaInfo").warning(msg);
    }
}
