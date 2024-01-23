package com.parallels.pa.rnd.jpa.type;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;

import java.util.UUID;

public class StringUuidJavaTypeDescriptor extends AbstractTypeDescriptor<String> {
	public static final StringUuidJavaTypeDescriptor INSTANCE = new StringUuidJavaTypeDescriptor();

	public StringUuidJavaTypeDescriptor() {
		super(String.class);
	}

	@Override
	public String fromString(String string) {
		return null;
	}

	@Override
	public <X> X unwrap(String value, Class<X> type, WrapperOptions options) {
		if (value == null) {
			return null;
		}

		if (UUID.class.isAssignableFrom(type)) {
			return type.cast(UUID.fromString(value));
		}

		throw unknownUnwrap(type);
	}

	@Override
	public <X> String wrap(X value, WrapperOptions options) {
		if (value == null) {
			return null;
		}

		if (UUID.class.isInstance(value)) {
			return value.toString();
		}

		return null;
	}
}
