/*Copyright [2018] [Kürsat Aydinli & Remo Schenker]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

package ase.rsse.app;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import ase.rsse.apirec.transactions.ITransactionConstants;
import ase.rsse.apirec.transactions.MethodMatch;
import ase.rsse.apirec.transactions.TransactionUtility;
import ase.rsse.utilities.IoUtility;
import cc.kave.commons.model.events.completionevents.CompletionEvent;
import cc.kave.commons.model.events.completionevents.ICompletionEvent;
import cc.kave.commons.model.ssts.impl.SST;

public class TransactionUtilityTest {
	
	public static File TRANSACTION_DIRECTORY;
	public static List<ICompletionEvent> TEST_COMPLETION_EVENTS;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TRANSACTION_DIRECTORY= new File(ITransactionConstants.TRANSACTION_DIRECTORY);
		TEST_COMPLETION_EVENTS = IoUtility.readEvent("C:\\workspaces\\ase_rsse\\apirec\\Events-170301-2\\2016-08-06\\2.zip");

		Assert.assertTrue(TEST_COMPLETION_EVENTS.size() > 200);
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		File testTransactionDirectory = new File(ITransactionConstants.TRANSACTION_DIRECTORY);
		for (File f: testTransactionDirectory.listFiles()) {
			if (f.getName().startsWith("test_")) {
				f.delete();
			}
		}
	}

	@Test
	public void testCreateMatch() {
		List<ICompletionEvent> ceSorted = TEST_COMPLETION_EVENTS.stream()
				.filter(Objects::nonNull)
				.sorted(Comparator.comparing(ICompletionEvent::getTriggeredAt, Comparator.reverseOrder()))
				.collect(Collectors.toList());

		// test case: everything stays the same -> no transaction created
		SST oldSST = (SST) ceSorted.get(0).getContext().getSST();
		SST newSST = oldSST;
		ArrayList<MethodMatch> matching = TransactionUtility.matchMethods(oldSST.getMethods(), newSST.getMethods());
		Assert.assertNotNull(matching);
		Assert.assertEquals(1, matching.get(0).getSimilarity(), 0.01);
		TransactionUtility.createTransaction((CompletionEvent) ceSorted.get(0), (CompletionEvent) ceSorted.get(0), 1);
		File[] testFiles = TRANSACTION_DIRECTORY.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				if (name.startsWith("test_")) {
					return true;
				}
				return false;
			}
		});
		// we do not persist empty transactions
		Assert.assertEquals(0, testFiles.length);
		
	}
	
	@Test
	public void testMatchWithDifferences() {
		List<ICompletionEvent> ceSorted = TEST_COMPLETION_EVENTS.stream()
				.filter(Objects::nonNull)
				.sorted(Comparator.comparing(ICompletionEvent::getTriggeredAt, Comparator.reverseOrder()))
				.collect(Collectors.toList());
		
		SST oldSST = (SST) ceSorted.get(185).getContext().getSST();
		SST newSST = (SST) ceSorted.get(192).getContext().getSST();
		
		ArrayList<MethodMatch> matching = TransactionUtility.matchMethods(oldSST.getMethods(), newSST.getMethods());
		Assert.assertNotNull(matching);
		
		TransactionUtility.createTransaction((CompletionEvent) ceSorted.get(185), (CompletionEvent) ceSorted.get(192), -100);
		File[] testFiles = TRANSACTION_DIRECTORY.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				if (name.endsWith("-100.json")) {
					return true;
				}
				return false;
			}
		});
		// we do not persist empty transactions
		Assert.assertTrue(testFiles.length > 0);
	}
}
