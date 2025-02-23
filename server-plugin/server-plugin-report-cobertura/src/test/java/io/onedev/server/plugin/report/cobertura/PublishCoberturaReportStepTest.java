package io.onedev.server.plugin.report.cobertura;

import com.google.common.io.Resources;
import io.onedev.commons.utils.FileUtils;
import io.onedev.commons.utils.TaskLogger;
import io.onedev.server.codequality.CoverageStatus;
import io.onedev.server.model.Build;
import io.onedev.server.plugin.report.coverage.FileCoverage;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.*;

public class PublishCoberturaReportStepTest {

    @Test
    public void test() {
		var inputDir = FileUtils.createTempDir();
		try {
			var build = new Build() {
				@Override
				public String getBlobPath(String filePath) {
					return filePath;
				}
			};
			try (
					var is = Resources.getResource(PublishCoberturaReportStepTest.class, "coverage1.xml").openStream();
					var os = new FileOutputStream(new File(inputDir, "coverage1.xml"))) {
				IOUtils.copy(is, os);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			try (
					var is = Resources.getResource(PublishCoberturaReportStepTest.class, "coverage2.xml").openStream();
					var os = new FileOutputStream(new File(inputDir, "coverage2.xml"))) {
				IOUtils.copy(is, os);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			try (
					var is = Resources.getResource(PublishCoberturaReportStepTest.class, "coverage3.xml").openStream();
					var os = new FileOutputStream(new File(inputDir, "coverage3.xml"))) {
				IOUtils.copy(is, os);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			var logger = new TaskLogger() {
				@Override
				public void log(String message, @Nullable String sessionId) {
				}
				
			};
			var step = new PublishCoberturaReportStep();
			step.setFilePatterns("*.xml");
			var result = step.process(build, inputDir, logger);
			var overallCoverage = result.getStats().getOverallCoverage();
			assertEquals(16, overallCoverage.getTotalBranches());
			assertEquals(8, overallCoverage.getCoveredBranches());
			assertEquals(2311, overallCoverage.getTotalLines());
			assertEquals(1992, overallCoverage.getCoveredLines());

			FileCoverage fileCoverage = null;
			boolean program1Found = false;
			boolean program2Found = false;
			for (var groupCoverage: result.getStats().getGroupCoverages()) {
				for (var each: groupCoverage.getFileCoverages()) {
					if (each.getBlobPath().endsWith("Program1.cs"))
						program1Found = true;
					if (each.getBlobPath().endsWith("Program2.cs"))
						program2Found = true;
					if (each.getBlobPath().equals("coverlet/supermario/Movement.cs")) 
						fileCoverage = each;						
				}
			}
			assertTrue(program1Found && program2Found);
			assertNotNull(fileCoverage);
			
			assertEquals(16, fileCoverage.getTotalBranches());
			assertEquals(9, fileCoverage.getCoveredBranches());
			assertEquals(36, fileCoverage.getTotalLines());
			assertEquals(33, fileCoverage.getCoveredLines());
			
			var statusesOfFile = result.getStatuses().get("coverlet/supermario/Movement.cs");
			
			var expected = new HashMap<Integer, CoverageStatus>();
			expected.put(37, CoverageStatus.COVERED);
			expected.put(6, CoverageStatus.COVERED);
			expected.put(38, CoverageStatus.COVERED);
			expected.put(7, CoverageStatus.COVERED);
			expected.put(39, CoverageStatus.COVERED);
			expected.put(8, CoverageStatus.COVERED);
			expected.put(42, CoverageStatus.COVERED);
			expected.put(11, CoverageStatus.COVERED);
			expected.put(43, CoverageStatus.PARTIALLY_COVERED);
			expected.put(12, CoverageStatus.COVERED);
			expected.put(44, CoverageStatus.COVERED);
			expected.put(13, CoverageStatus.COVERED);
			expected.put(14, CoverageStatus.PARTIALLY_COVERED);
			expected.put(15, CoverageStatus.COVERED);
			expected.put(16, CoverageStatus.COVERED);
			expected.put(17, CoverageStatus.COVERED);
			expected.put(19, CoverageStatus.COVERED);
			expected.put(20, CoverageStatus.PARTIALLY_COVERED);
			expected.put(25, CoverageStatus.COVERED);
			expected.put(26, CoverageStatus.COVERED);
			expected.put(28, CoverageStatus.COVERED);
			expected.put(27, CoverageStatus.COVERED);
			
			assertEquals(expected, statusesOfFile);
			
			for (var groupCoverage: result.getStats().getGroupCoverages()) {
				for (var each: groupCoverage.getFileCoverages()) {
					if (each.getBlobPath().endsWith("help.py")) 
						assertEquals(-1, each.getBranchPercentage());
				}
			}
			assertEquals(CoverageStatus.NOT_COVERED, result.getStatuses().get("/example/src/requests/help.py").get(49));
		} finally {
			FileUtils.deleteDir(inputDir);
		}
    }

}