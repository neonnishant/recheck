package de.retest.recheck.review.ignore.matcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.retest.recheck.ui.Path;
import de.retest.recheck.ui.descriptors.Element;
import de.retest.recheck.ui.descriptors.IdentifyingAttributes;
import de.retest.recheck.ui.descriptors.MutableAttributes;

class ElementXPathMatcherTest {

	private ElementXPathMatcher matcher;

	@BeforeEach
	void setUp() {
		final Element element = Element.create( "retestId", mock( Element.class ),
				IdentifyingAttributes.create( Path.fromString( "html[1]/div[1]" ), "DIV" ),
				new MutableAttributes().immutable() );
		matcher = new ElementXPathMatcher( element );
	}

	@Test
	void should_match_when_xpath_is_equal() {
		final Element element = Element.create( "retestId", mock( Element.class ),
				IdentifyingAttributes.create( Path.fromString( "html[1]/div[1]" ), "DIV" ),
				new MutableAttributes().immutable() );

		assertThat( matcher ).accepts( element );
	}

	@Test
	void should_match_with_xpath_from_Chrome() {
		final Element element = Element.create( "retestId", mock( Element.class ),
				IdentifyingAttributes.create( Path.fromString( "html[1]/body[1]/div[3]/section[1]" ), "DIV" ),
				new MutableAttributes().immutable() );

		final ElementXPathMatcher matcher =
				new ElementXPathMatcher.ElementXpathMatcherLoader().load( "xpath=/html/body/div[3]/section" ).get();
		assertThat( matcher ).accepts( element );
	}

	@Test
	void should_match_with_leading_slash() {
		final Element element = Element.create( "retestId", mock( Element.class ),
				IdentifyingAttributes.create( Path.fromString( "html" ), "DIV" ), new MutableAttributes().immutable() );

		final ElementXPathMatcher matcher =
				new ElementXPathMatcher.ElementXpathMatcherLoader().load( "xpath=/html[1]" ).get();
		assertThat( matcher ).accepts( element );
	}

	@Test
	void should_not_match_when_xpath_is_not_equal() {
		final Element element = Element.create( "retestId", mock( Element.class ),
				IdentifyingAttributes.create( Path.fromString( "html[1]/div[2]" ), "DIV" ),
				new MutableAttributes().immutable() );

		assertThat( matcher ).rejects( element );
	}
}
