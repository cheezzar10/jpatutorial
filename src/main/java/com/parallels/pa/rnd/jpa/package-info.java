@org.hibernate.annotations.GenericGenerator(
        name = "cached-sequence-generator",
        strategy = "enhanced-sequence",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "cached_sequence"),
                @org.hibernate.annotations.Parameter(name = "increment_size", value = "100"),
                @org.hibernate.annotations.Parameter(name = "optimizer", value = "pooled-lo")
        })
package com.parallels.pa.rnd.jpa;