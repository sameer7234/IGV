/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.cnv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.broad.igv.data.seg.ReferenceSegment;
import org.broad.igv.data.seg.Segment;
import org.broad.igv.data.seg.SegmentedDataSet;
import org.broad.igv.data.seg.SummarySegment;
import org.broad.igv.feature.FeatureUtils;
import org.broad.igv.feature.LocusScore;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.track.TrackProperties;
import org.broad.igv.track.TrackType;
import org.broad.igv.ui.IGV;

/**
 *
 * @author Chantal
 */
public class SegmentedCustomCnvDataSet implements SegmentedDataSet {

    TrackType trackType = TrackType.COPY_NUMBER;
    float dataMax = -Float.MAX_VALUE;
    float dataMin = Float.MAX_VALUE;
    /**
     * Assume data is non-log value until suggested otherwise by the precense of
     * negative numbers. TODO This is a fragile assumption, the user should
     * input this information directly.
     */
    private boolean logNormalized = false;
    /**
     * Map of [heading -> [chr -> [list of chrSegments]]]
     */
    private Map<String, Map<String, List<LocusScore>>> segments = new HashMap();
    /**
     * Set of chromosomes represented in this dataset
     */
    private Set<String> chromosomes = new HashSet();
    private List<String> headings = new ArrayList();
    private Map<String, List<LocusScore>> wholeGenomeScoresCache = new HashMap();
    private TrackProperties trackProperties;
    Genome genome;
    int count;

    public SegmentedCustomCnvDataSet(String id, CnvData data) {
        this.genome = IGV.getInstance().getGenomeManager().getCurrentGenome();
        for (CnvDataPoint point : data.getPoints()) {
            /// add att map?
            this.addSegment(id, "chr" + point.chr, (int) point.pos, (int) point.end + 1, (float) point.ratio, point.clone, null);
        }
        sortLists();
    }

    public void sortLists() {
        for (Map.Entry<String, Map<String, List<LocusScore>>> sampleEntry : segments.entrySet()) {
            for (Map.Entry<String, List<LocusScore>> chrEntry : sampleEntry.getValue().entrySet()) {
                List<LocusScore> tmp = chrEntry.getValue();
                FeatureUtils.sortFeatureList(tmp);
            }
        }
    }

    /**
     *
     */
    public void addSegment(String heading, String c, int start, int end, float value, String desc,HashMap<String,String> atts) {

        count++;
        String chr = genome == null ? c : genome.getChromosomeAlias(c);
        // p("Adding seg to chr "+chr);
        Map<String, List<LocusScore>> chrSegments = segments.get(heading);
        if (chrSegments == null) {
            headings.add(heading);
            chrSegments = new HashMap();
            segments.put(heading, chrSegments);
        }

        List<LocusScore> segmentList = chrSegments.get(chr);
        if (segmentList == null) {
            segmentList = new ArrayList<LocusScore>();
            chrSegments.put(chr, segmentList);
        }
        Segment seg = new Segment(chr, start, start, end, end, value, desc, atts);
        segmentList.add(seg);
        dataMax = Math.max(dataMax, value);
        dataMin = Math.min(dataMin, value);
        if (value < 0) {
            logNormalized = true;
        }
        //  if (count % 100 ==0) p("Got segment: "+seg+", max="+dataMax+", min="+dataMin);

        chromosomes.add(chr);

    }

    private void p(String s) {
        //   System.out.println("SegmentedCustomCnvDataSet: "+s);
    }

    /**
     * Method description
     *
     * @return
     */
    public Set<String> getChromosomes() {
        return chromosomes;
    }

    /**
     * Method description
     *
     * @param heading
     * @param chr
     * @return
     */
    @Override
    public List<LocusScore> getSegments(String heading, String chr) {
        //    p("Getting segments "+heading+"/"+ chr);
        Map<String, List<LocusScore>> chrSegments = segments.get(heading);
        //    p("getSegments: Got: "+chrSegments.size()+" for heading "+heading);
        List<LocusScore> res = (chrSegments == null) ? null : chrSegments.get(chr);
        // p("Got locus scores: "+res+" for chr "+chr);
        return res;
    }

    @Override
    public List<String> getSampleNames() {
        return headings;
    }

    /**
     * Method description
     *
     * @return
     */
    @Override
    public TrackType getType() {
        return trackType;
    }

    /**
     * Method description
     *
     * @return
     */
    @Override
    public boolean isLogNormalized() {
        return logNormalized;
    }

    /**
     * Method description
     *
     * @param chr
     * @return
     */
    @Override
    public double getDataMax(String chr) {
        return dataMax;
    }

    /**
     * Method description
     *
     * @param chr
     * @return
     */
    @Override
    public double getDataMin(String chr) {
        return dataMin;
    }

    /**
     * Method description
     *
     * @param heading
     * @return
     */
    @Override
    public List<LocusScore> getWholeGenomeScores(String heading) {
        p("getWholeGenomeScores " + heading);

        List<LocusScore> wholeGenomeScores = wholeGenomeScoresCache.get(heading);
        if ((wholeGenomeScores == null) || wholeGenomeScores.isEmpty()) {

            // Compute the smallest concievable feature that could be viewed on the
            // largest screen.  Be conservative.   The smallest feature is one at
            // the screen resolution scale in <chr units> / <pixel>
            double minFeatureSize = 0; // ((double) genome.getLength()) / (maxScreenSize * locationUnit);

            long offset = 0;
            wholeGenomeScores = new ArrayList(1000);

            for (String chr : genome.getLongChromosomeNames()) {
                List<LocusScore> chrSegments = getSegments(heading, chr);
                if (chrSegments != null) {
                    int lastgEnd = -1;
                    //      p("getWholeGenomeScores: got "+chrSegments.size()+" for "+heading+"/"+chr);
                    for (LocusScore score : chrSegments) {
                        Segment seg = (Segment) score;
                        int gStart = genome.getGenomeCoordinate(chr, seg.getStart());
                        int gEnd = genome.getGenomeCoordinate(chr, seg.getEnd());
                        if ((gEnd - gStart) >= minFeatureSize) {
                            Segment s = null;
                            if (score instanceof ReferenceSegment) {
                              //  p("Got ref segment: "+score);
                                s = new ReferenceSegment(chr, gStart, gStart, gEnd,
                                        gEnd, seg.getScore(), seg.getDescription(), seg.getAttributes());
                            }
                            else if (score instanceof SummarySegment) {
                              //  p("Got ref segment: "+score);
                                s = new SummarySegment(chr, gStart, gStart, gEnd,
                                        gEnd, seg.getScore(), seg.getDescription(), seg.getAttributes());
                            } else {
                                s = new Segment(chr, gStart, gStart, gEnd,
                                        gEnd, seg.getScore(), seg.getDescription(), seg.getAttributes());
                            }
                            wholeGenomeScores.add(s);
                        }
                        //           else p("Too small: "+(gEnd-gStart));
                    }

                }
                offset += genome.getChromosome(chr).getLength();
            }
            wholeGenomeScoresCache.put(heading, wholeGenomeScores);
        }
        return wholeGenomeScores;

    }

    /**
     * Method description
     *
     * @return
     */
    public TrackType getTrackType() {
        return trackType;
    }

    /**
     * Method description
     *
     * @param trackType
     */
    public void setTrackType(TrackType trackType) {
        this.trackType = trackType;
    }

    public void setTrackProperties(TrackProperties props) {
        this.trackProperties = props;
    }

    public TrackProperties getTrackProperties() {
        if (trackProperties == null) {
            trackProperties = new TrackProperties();
        }
        return trackProperties;
    }
}
