package org.broadinstitute.dropseqrna.annotation;

import htsjdk.samtools.SAMSequenceDictionary;
import org.apache.commons.lang.builder.CompareToBuilder;

import java.util.Comparator;

public class GenomicOrderComparator implements Comparator <GTFRecord> {
	SAMSequenceDictionary refDict;
	
	public GenomicOrderComparator (SAMSequenceDictionary refDict) {
		this.refDict = refDict;
	}
	
	public int compare(GTFRecord g1, GTFRecord g2) {
		int i1 = refDict.getSequenceIndex(g1.getChromosome());
		int i2 = refDict.getSequenceIndex(g2.getChromosome());
		return new CompareToBuilder().append(i1, i2)
				.append(g1.getStart(), g2.getStart())
				.append(g1.getEnd(), g2.getEnd())
                .append(g1.getTranscriptType(), g2.getTranscriptType())
				.toComparison();
	}
}