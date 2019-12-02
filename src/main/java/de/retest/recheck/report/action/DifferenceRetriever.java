package de.retest.recheck.report.action;

import java.util.List;

import de.retest.recheck.ui.diff.RootElementDifference;
import de.retest.recheck.ui.diff.StateDifference;

@FunctionalInterface
public interface DifferenceRetriever {

	StateDifference getStateDifference();

	default boolean isNull() {
		return getStateDifference() == null;
	}

	static DifferenceRetriever empty() {
		return () -> null;
	}

	static DifferenceRetriever of( final List<RootElementDifference> differences ) {
		return of( new StateDifference( differences ) );
	}

	static DifferenceRetriever of( final StateDifference difference ) {
		return () -> difference;
	}
}
