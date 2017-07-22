package com.parallels.pa.rnd.jpa.boot;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

public class PersistenceUnitDescriptorBuilder {
	private String persistenceUnitName;
	private List<String> managedClassNames = new ArrayList<>();
	
	public void setPersistentUnitName(String unitName) {
		persistenceUnitName = unitName;
	}

	public void addManagedClass(Class<?> managedClass) {
		managedClassNames.add(managedClass.getName());
	}

	public PersistenceUnitDescriptor build() {
		return new PersistenceUnitDescriptor() {
			@Override
			public ClassLoader getClassLoader() {
				return null;
			}

			@Override
			public List<URL> getJarFileUrls() {
				return Collections.emptyList();
			}

			@Override
			public Object getJtaDataSource() {
				return null;
			}

			@Override
			public List<String> getManagedClassNames() {
				return managedClassNames;
			}

			@Override
			public List<String> getMappingFileNames() {
				return Collections.emptyList();
			}

			@Override
			public String getName() {
				return persistenceUnitName;
			}

			@Override
			public Object getNonJtaDataSource() {
				return null;
			}

			@Override
			public URL getPersistenceUnitRootUrl() {
				return null;
			}

			@Override
			public Properties getProperties() {
				return new Properties();
			}

			@Override
			public String getProviderClassName() {
				return "org.hibernate.ejb.HibernatePersistence";
			}

			@Override
			public SharedCacheMode getSharedCacheMode() {
				return SharedCacheMode.ENABLE_SELECTIVE;
			}

			@Override
			public ClassLoader getTempClassLoader() {
				return null;
			}

			@Override
			public PersistenceUnitTransactionType getTransactionType() {
				return PersistenceUnitTransactionType.RESOURCE_LOCAL;
			}

			@Override
			public ValidationMode getValidationMode() {
				return ValidationMode.AUTO;
			}

			@Override
			public boolean isExcludeUnlistedClasses() {
				return true;
			}

			@Override
			public boolean isUseQuotedIdentifiers() {
				return false;
			}

			@Override
			public void pushClassTransformer(EnhancementContext ctx) {
				
			}
		};
	}
}
