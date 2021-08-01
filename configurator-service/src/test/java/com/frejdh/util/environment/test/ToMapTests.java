package com.frejdh.util.environment.test;

import com.frejdh.util.environment.Config;
import org.junit.Test;

public class ToMapTests extends AbstractTests {

	@Test
	public void toPropertyMapWorks() {
		String propertyKey = "env.test1";
		System.out.println(Config.getMap(propertyKey));
	}

}
