package de.retest.recheck.report;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import de.retest.recheck.NoGoldenMasterActionReplayResult;
import de.retest.recheck.ignore.Filter;
import de.retest.recheck.report.action.ActionReplayData;
import de.retest.recheck.ui.actions.ExceptionWrapper;
import de.retest.recheck.ui.actions.TargetNotFoundException;
import de.retest.recheck.ui.descriptors.Element;
import de.retest.recheck.ui.descriptors.SutState;
import de.retest.recheck.ui.diff.AttributesDifference;
import de.retest.recheck.ui.diff.ElementDifference;
import de.retest.recheck.ui.diff.IdentifyingAttributesDifference;
import de.retest.recheck.ui.diff.InsertedDeletedElementDifference;
import de.retest.recheck.ui.diff.LeafDifference;
import de.retest.recheck.ui.diff.RootElementDifference;
import de.retest.recheck.ui.diff.StateDifference;
import de.retest.recheck.util.StreamUtil;

public class TestReportFilter {

	private TestReportFilter() {}

	public static TestReport filter( final TestReport report, final Filter filter ) {
		final TestReport newTestReport = new TestReport();
		for ( final SuiteReplayResult suiteReplayResult : report.getSuiteReplayResults() ) {
			newTestReport.addSuite( filter( suiteReplayResult, filter ) );
		}
		return newTestReport;
	}

	static SuiteReplayResult filter( final SuiteReplayResult suiteReplayResult, final Filter filter ) {
		final SuiteReplayResult newSuiteReplayResult = new SuiteReplayResult( suiteReplayResult.getName(),
				suiteReplayResult.getSuiteNr(), suiteReplayResult.getExecSuiteSutVersion(),
				suiteReplayResult.getSuiteUuid(), suiteReplayResult.getReplaySutVersion() );
		for ( final TestReplayResult testReplayResult : suiteReplayResult.getTestReplayResults() ) {
			newSuiteReplayResult.addTest( filter( testReplayResult, filter ) );
		}
		return newSuiteReplayResult;
	}

	public static TestReplayResult filter( final TestReplayResult testReplayResult, final Filter filter ) {
		final TestReplayResult newTestReplayResult =
				new TestReplayResult( testReplayResult.getName(), testReplayResult.getTestNr() );
		for ( final ActionReplayResult actionReplayResult : testReplayResult.getActionReplayResults() ) {
			newTestReplayResult.addAction( filter( actionReplayResult, filter ) );
		}
		return newTestReplayResult;
	}

	static ActionReplayResult filter( final ActionReplayResult actionReplayResult, final Filter filter ) {
		if ( actionReplayResult instanceof NoGoldenMasterActionReplayResult ) {
			return actionReplayResult;
		}

		final ActionReplayData data = ActionReplayData.withTarget( actionReplayResult.getDescription(),
				actionReplayResult.getTargetComponent(), actionReplayResult.getGoldenMasterPath() );
		final ExceptionWrapper error = actionReplayResult.getThrowableWrapper();
		final TargetNotFoundException targetNotFound =
				(TargetNotFoundException) actionReplayResult.getTargetNotFoundException();
		final StateDifference newStateDifference = filter( actionReplayResult.getStateDifference(), filter );
		final long actualDuration = actionReplayResult.getDuration();
		final SutState actualState = new SutState( actionReplayResult.getWindows() );
		return ActionReplayResult.createActionReplayResult( data, error, targetNotFound, newStateDifference,
				actualDuration, actualState );
	}

	static StateDifference filter( final StateDifference stateDifference, final Filter filter ) {
		if ( stateDifference == null || stateDifference.getRootElementDifferences().isEmpty() ) {
			return stateDifference;
		}
		final List<RootElementDifference> newRootElementDifferences =
				filter( stateDifference.getRootElementDifferences(), filter );
		return new StateDifference( newRootElementDifferences, stateDifference.getDurationDifference() );
	}

	static List<RootElementDifference> filter( final List<RootElementDifference> rootElementDifferences,
			final Filter filter ) {
		return rootElementDifferences.stream() //
				.map( rootElementDifference -> filter( rootElementDifference, filter ) ) //
				.flatMap( StreamUtil::optionalToStream ) //
				.collect( Collectors.toList() );
	}

	static Optional<RootElementDifference> filter( final RootElementDifference rootElementDifference,
			final Filter filter ) {
		return filter( rootElementDifference.getElementDifference(), filter ) //
				.map( newElementDifference -> new RootElementDifference( newElementDifference,
						rootElementDifference.getExpectedDescriptor(), rootElementDifference.getActualDescriptor() ) );
	}

	static Optional<ElementDifference> filter( final ElementDifference elementDiff, final Filter filter ) {
		AttributesDifference attributesDifference = elementDiff.getAttributesDifference();
		LeafDifference identifyingAttributesDifference = elementDiff.getIdentifyingAttributesDifference();
		Collection<ElementDifference> childDifferences = elementDiff.getChildDifferences();

		if ( elementDiff.hasAttributesDifferences() ) {
			attributesDifference = filter( elementDiff.getElement(), elementDiff.getAttributesDifference(), filter );
		}

		if ( elementDiff.hasIdentAttributesDifferences() ) {
			identifyingAttributesDifference = filter( elementDiff.getElement(),
					(IdentifyingAttributesDifference) elementDiff.getIdentifyingAttributesDifference(), filter );
		} else if ( elementDiff.isInsertionOrDeletion() ) {
			identifyingAttributesDifference = filter(
					(InsertedDeletedElementDifference) elementDiff.getIdentifyingAttributesDifference(), filter );
		}

		if ( elementDiff.hasChildDifferences() ) {
			childDifferences = filter( elementDiff.getChildDifferences(), filter );
		}

		final ElementDifference newElementDiff =
				new ElementDifference( elementDiff.getElement(), attributesDifference, identifyingAttributesDifference,
						elementDiff.getExpectedScreenshot(), elementDiff.getActualScreenshot(), childDifferences );
		final boolean anyOwnOrChildDiffs = newElementDiff.hasAnyDifference() || newElementDiff.hasChildDifferences();
		return anyOwnOrChildDiffs ? Optional.of( newElementDiff ) : Optional.empty();
	}

	static Collection<ElementDifference> filter( final Collection<ElementDifference> elementDifferences,
			final Filter filter ) {
		return elementDifferences.stream() //
				.map( elementDifference -> filter( elementDifference, filter ) ) //
				.flatMap( StreamUtil::optionalToStream ) //
				.collect( Collectors.toList() );
	}

	static IdentifyingAttributesDifference filter( final Element element,
			final IdentifyingAttributesDifference identAttributesDiff, final Filter filter ) {
		return identAttributesDiff.getAttributeDifferences().stream() //
				.filter( diff -> !filter.matches( element, diff ) ) //
				.collect( Collectors.collectingAndThen( Collectors.toList(), diffs -> diffs.isEmpty() //
						? null // expected by ElementDifference
						: new IdentifyingAttributesDifference( element.getIdentifyingAttributes(), diffs ) ) );
	}

	static InsertedDeletedElementDifference filter( final InsertedDeletedElementDifference insertedDeletedDiff,
			final Filter filter ) {
		final Element insertedDeleted =
				insertedDeletedDiff.isInserted() ? insertedDeletedDiff.getActual() : insertedDeletedDiff.getExpected();
		return filter.matches( insertedDeleted ) ? null : insertedDeletedDiff;
	}

	static AttributesDifference filter( final Element element, final AttributesDifference attributesDiff,
			final Filter filter ) {
		return attributesDiff.getDifferences().stream() //
				.filter( diff -> !filter.matches( element, diff ) ) //
				.collect( Collectors.collectingAndThen( Collectors.toList(), diffs -> diffs.isEmpty() //
						? null // expected by ElementDifference
						: new AttributesDifference( diffs ) ) );
	}
}
