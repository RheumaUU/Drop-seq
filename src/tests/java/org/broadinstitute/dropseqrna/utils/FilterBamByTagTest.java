/*
 * MIT License
 *
 * Copyright 2017 Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.broadinstitute.dropseqrna.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.annotations.Test;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordSetBuilder;
import junit.framework.Assert;

public class FilterBamByTagTest {

	private static File PAIRED_INPUT_FILE=new File ("testdata/org/broadinstitute/dropseq/utils/paired_reads_tagged.bam");
	private static File UNPAIRED_INPUT_FILE=new File ("testdata/org/broadinstitute/dropseq/utils/unpaired_reads_tagged.bam");
	private static File PAIRED_INPUT_FILE_FILTERED=new File ("testdata/org/broadinstitute/dropseq/utils/paired_reads_tagged_filtered.bam");
	private static File UNPAIRED_INPUT_FILE_FILTERED=new File ("testdata/org/broadinstitute/dropseq/utils/unpaired_reads_tagged_filtered.bam");
	private static File PAIRED_INPUT_CELL_BARCODES=new File ("testdata/org/broadinstitute/dropseq/utils/paired_reads_tagged.cell_barcodes.txt");

	private static File UNPAIRED_INPUT_FILE_FILTERED_AAAGTAGAGTGG=new File ("testdata/org/broadinstitute/dropseq/utils/unpaired_reads_tagged_filtered_AAAGTAGAGTGG.bam");

	@Test (enabled=true)
	public void testDoWorkPaired () {
		FilterBamByTag f = new FilterBamByTag();
		f.INPUT=PAIRED_INPUT_FILE;
		f.OUTPUT=getTempReportFile("paired_input", ".bam");
		f.TAG="XC";
		f.PAIRED_MODE=true;
		f.TAG_VALUES_FILE=PAIRED_INPUT_CELL_BARCODES;
		f.OUTPUT.deleteOnExit();
		int result = f.doWork();
		Assert.assertEquals(0, result);

		CompareBAMTagValues cbtv = new CompareBAMTagValues();
		cbtv.INPUT_1=PAIRED_INPUT_FILE_FILTERED;
		cbtv.INPUT_2=f.OUTPUT;
		List<String> tags = new ArrayList<>();
		tags.add("XC");
		cbtv.TAGS=tags;
		int r = cbtv.doWork();
		Assert.assertTrue(r==0);

	}

	@Test
	public void testDoWorkUnPaired () {
		FilterBamByTag f = new FilterBamByTag();
		f.INPUT=UNPAIRED_INPUT_FILE;
		f.OUTPUT=getTempReportFile("unpaired_input", ".bam");
		f.TAG="XC";
		f.PAIRED_MODE=false;
		f.TAG_VALUES_FILE=PAIRED_INPUT_CELL_BARCODES;
		f.OUTPUT.deleteOnExit();
		int result = f.doWork();
		Assert.assertEquals(0, result);

		CompareBAMTagValues cbtv = new CompareBAMTagValues();
		cbtv.INPUT_1=UNPAIRED_INPUT_FILE_FILTERED;
		cbtv.INPUT_2=f.OUTPUT;
		List<String> tags = new ArrayList<>();
		tags.add("XC");
		cbtv.TAGS=tags;
		int r = cbtv.doWork();
		Assert.assertTrue(r==0);

		// test alternate path without tag values file.
		f.INPUT=UNPAIRED_INPUT_FILE;
		f.OUTPUT=getTempReportFile("unpaired_input_single_cell", ".bam");
		f.TAG="XC";
		f.TAG_VALUE="AAAGTAGAGTGG";
		f.TAG_VALUES_FILE=null;
		f.PAIRED_MODE=false;
		f.OUTPUT.deleteOnExit();
		result = f.doWork();
		Assert.assertEquals(0, result);


		cbtv.INPUT_1=UNPAIRED_INPUT_FILE_FILTERED_AAAGTAGAGTGG;
		cbtv.INPUT_2=f.OUTPUT;
		cbtv.TAGS=tags;
		r = cbtv.doWork();
		Assert.assertTrue(r==0);


	}


	@Test(enabled = true)
	public void filterReadTest() {
		SAMRecord readHasAttribute = new SAMRecord(null);
		String tag = "XT";
		readHasAttribute.setAttribute(tag, "1");

		Set<String> values = new HashSet<>();
		values.add("1");

		SAMRecord readNoAttribute = new SAMRecord(null);

		FilterBamByTag t = new FilterBamByTag();
		// read has attribute, accept any value, want to retain read.
		boolean flag1 = t.filterRead(readHasAttribute, tag, null, true);
		Assert.assertFalse(flag1);

		// read has attribute, accept any value, want to filter read.
		boolean flag2 = t.filterRead(readHasAttribute, tag, null, false);
		Assert.assertTrue(flag2);

		// read has attribute, accept certain value, want to retain read.
		boolean flag3 = t.filterRead(readHasAttribute, tag, values, true);
		Assert.assertFalse(flag3);

		// read has attribute, accept certain value, want to filter read.
		boolean flag4 = t.filterRead(readHasAttribute, tag, values, false);
		Assert.assertTrue(flag4);

		// read does not have attribute, accept any value, want to retain read.
		boolean flag5 = t.filterRead(readNoAttribute, tag, null, true);
		Assert.assertTrue(flag5);

		// read does not have attribute, accept any value, want to filter read.
		boolean flag6 = t.filterRead(readNoAttribute, tag, null, false);
		Assert.assertFalse(flag6);

		// read does not have attribute, accept certain value, want to retain read.
		boolean flag7 = t.filterRead(readNoAttribute, tag, values, true);
		Assert.assertTrue(flag7);

		// read does not have attribute, accept certain value, want to filter read.
		boolean flag8 = t.filterRead(readNoAttribute, tag, values, false);
		Assert.assertFalse(flag8);

	}

	/**
	 * Returns a paired read, first of pair in the first position of the list, 2nd of pair in the 2nd position.
	 * @return
	 */
	private List<SAMRecord> getPairedRead () {
		List<SAMRecord> result = new ArrayList<> ();

		SAMRecordSetBuilder builder = new SAMRecordSetBuilder();
		builder.addUnmappedPair("test");
		Collection<SAMRecord> recs = builder.getRecords();

		for (SAMRecord r: recs) {
			if (r.getFirstOfPairFlag()) result.add(0, r);
			if (r.getSecondOfPairFlag()) result.add(1, r);
		}
		return (result);

	}

	@Test(enabled = true)
	public void filterByReadNumberTest() {
		FilterBamByTag t = new FilterBamByTag();

		// record paired and read is 1st
		List<SAMRecord> recs = getPairedRead ();
		SAMRecord recFirstPaired = recs.get(0);
		SAMRecord recSecondPaired = recs.get(1);

		boolean flag1= t.retainByReadNumber(recFirstPaired, 1);
		boolean flag2= t.retainByReadNumber(recFirstPaired, 2);
		Assert.assertTrue(flag1);
		Assert.assertFalse(flag2);

		// record paired and read is 2st
		recSecondPaired.setProperPairFlag(true);
		recSecondPaired.setSecondOfPairFlag(true);
		flag1= t.retainByReadNumber(recSecondPaired, 1);
		flag2= t.retainByReadNumber(recSecondPaired, 2);
		Assert.assertTrue(flag2);
		Assert.assertFalse(flag1);

		// record unpaired and read is 1st
		SAMRecordSetBuilder builder = new SAMRecordSetBuilder();
		builder.addUnmappedFragment("foo");
		SAMRecord recFirstUnPaired = builder.getRecords().iterator().next();

		flag1= t.retainByReadNumber(recFirstUnPaired, 1);
		flag2= t.retainByReadNumber(recFirstPaired, 2);
		Assert.assertTrue(flag1);
		Assert.assertFalse(flag2);
	}

	private File getTempReportFile (final String prefix, final String suffix) {
		File tempFile=null;

		try {
			tempFile = File.createTempFile(prefix, suffix);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return tempFile;
	}

	@Test
	public void testArgErrors () {
		FilterBamByTag f = new FilterBamByTag();
		f.INPUT=PAIRED_INPUT_FILE;
		f.OUTPUT=getTempReportFile("paired_input", ".bam");
		f.PAIRED_MODE=true;
		f.OUTPUT.deleteOnExit();
		Assert.assertSame(1, f.doWork());

	}

}
