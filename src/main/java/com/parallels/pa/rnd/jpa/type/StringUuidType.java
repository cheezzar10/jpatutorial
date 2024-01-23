package com.parallels.pa.rnd.jpa.type;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;

public class StringUuidType extends AbstractSingleColumnStandardBasicType<String> {
	public StringUuidType() {
		super(StringUuidSqlTypeDescriptor.INSTANCE, StringUuidJavaTypeDescriptor.INSTANCE);
	}

	@Override
	public String getName() {
		return "varchar-uuid";
	}
}
