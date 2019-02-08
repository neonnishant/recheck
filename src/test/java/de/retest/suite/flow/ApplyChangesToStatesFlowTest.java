package de.retest.suite.flow;

import static de.retest.recheck.persistence.RecheckStateFileProviderImpl.RECHECK_PROJECT_ROOT;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.retest.recheck.configuration.Configuration;
import de.retest.recheck.persistence.Persistence;
import de.retest.recheck.persistence.PersistenceFactory;
import de.retest.recheck.persistence.xml.util.StdXmlClassesProvider;
import de.retest.recheck.ui.descriptors.Element;
import de.retest.recheck.ui.descriptors.SutState;
import de.retest.recheck.ui.diff.AttributeDifference;
import de.retest.recheck.ui.review.ReviewResult;
import de.retest.recheck.ui.review.SuiteChangeSet;
import de.retest.recheck.util.junit.vintage.SystemProperty;

public class ApplyChangesToStatesFlowTest {

	private static final String SUITE = "ApplyChangesToStatesFlowTest";
	private static final String TEST = "check_GUI_with_review_license";
	private static final String ACTION = "launch";
	private static final String FILE = "src/test/resources/recheck/" + SUITE + "/" + TEST + "." + ACTION + ".recheck";

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public SystemProperty recheckDirProp = new SystemProperty( RECHECK_PROJECT_ROOT );

	@Before
	public void setUp() throws Exception {
		Configuration.resetRetest();
		recheckDirProp.setValue( "/" );
	}

	@Test
	public void apply_should_apply_changes_to_states() throws Exception {
		final PersistenceFactory persistenceFactory =
				new PersistenceFactory( new HashSet<>( Arrays.asList( StdXmlClassesProvider.getXmlDataClasses() ) ) );

		final Persistence<SutState> persistence = persistenceFactory.getPersistence();
		final File originalFile = new File( FILE );
		final File temporaryFile = temporaryFolder.newFile( "check_GUI_with_review_license.launch.recheck" );

		FileUtils.copyFile( originalFile, temporaryFile );
		final SutState before = persistence.load( temporaryFile.toURI() );

		final ReviewResult reviewResult = new ReviewResult();
		final SuiteChangeSet acceptedChanges = reviewResult.createSuiteChangeSet( SUITE, "" );
		final Element toChange = before.getRootElements().get( 0 ).getContainedElements().get( 0 )
				.getContainedElements().get( 0 ).getContainedElements().get( 0 ).getContainedElements().get( 0 )
				.getContainedElements().get( 0 );
		acceptedChanges.createTestChangeSet().createActionChangeSet( ACTION, temporaryFile.getPath() )
				.getAttributesChanges()
				.add( toChange.getIdentifyingAttributes(), new AttributeDifference( "enabled", "false", "true" ) );

		ApplyChangesToStatesFlow.apply( persistence, acceptedChanges );
		final SutState after = persistence.load( temporaryFile.toURI() );
		final Element changed = after.getRootElements().get( 0 ).getContainedElements().get( 0 ).getContainedElements()
				.get( 0 ).getContainedElements().get( 0 ).getContainedElements().get( 0 ).getContainedElements()
				.get( 0 );

		assertThat( changed.getAttributes().get( "enabled" ) ).isEqualTo( true );

		// even more advanced: check if difference between before and after is acceptedChanges!
	}
}