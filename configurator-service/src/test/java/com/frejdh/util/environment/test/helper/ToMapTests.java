package com.frejdh.util.environment.test.helper;

import com.frejdh.util.environment.Config;
import org.junit.jupiter.api.Test;

public class ToMapTests extends AbstractTests {

	@Test
	public void toPropertyMapWorks() {
		String propertyKey = "env.test1";
		System.out.println(Config.getMap(propertyKey));
	}

}
