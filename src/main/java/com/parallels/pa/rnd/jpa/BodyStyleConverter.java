package com.parallels.pa.rnd.jpa;

import javax.persistence.AttributeConverter;

public class BodyStyleConverter implements AttributeConverter<BodyStyle, Character> {
	@Override
	public Character convertToDatabaseColumn(BodyStyle style) {
		if (style == null) {
			return null;
		}

		switch (style) {
		case SEDAN: return 's';
		case KOMBI: return 'k';
		case COUPE: return 'c';
		case HATCHBACK: return 'h';
		default: throw new IllegalArgumentException("unsuppoted body style: " + style);
		}
	}

	@Override
	public BodyStyle convertToEntityAttribute(Character styleCode) {
		if (styleCode == null) {
			return null;
		}

		switch (styleCode) {
		case 's': return BodyStyle.SEDAN;
		case 'k': return BodyStyle.KOMBI;
		case 'c': return BodyStyle.COUPE;
		case 'h': return BodyStyle.HATCHBACK;
		default: throw new IllegalArgumentException("invalid body style code: " + styleCode);
		}
	}
}
