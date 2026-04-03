@SuppressWarnings({ "requires-automatic"})
open module net.dreamlu.mica.net.utils {
	requires transitive org.slf4j;
	requires static fastjson;
	requires static com.alibaba.fastjson2;
	requires static com.fasterxml.jackson.annotation;
	requires static com.fasterxml.jackson.core;
	requires static com.fasterxml.jackson.databind;
	requires static tools.jackson.core;
	requires static tools.jackson.databind;
	requires static com.google.gson;
	requires static cn.hutool.json;
	requires static snack3;
	exports net.dreamlu.mica.net.utils;
	exports net.dreamlu.mica.net.utils.buffer;
	exports net.dreamlu.mica.net.utils.cache;
	exports net.dreamlu.mica.net.utils.collection;
	exports net.dreamlu.mica.net.utils.compression;
	exports net.dreamlu.mica.net.utils.hutool;
	exports net.dreamlu.mica.net.utils.json;
	exports net.dreamlu.mica.net.utils.mica;
	exports net.dreamlu.mica.net.utils.page;
	exports net.dreamlu.mica.net.utils.prop;
	exports net.dreamlu.mica.net.utils.queue;
	exports net.dreamlu.mica.net.utils.thread;
	exports net.dreamlu.mica.net.utils.thread.pool;
	exports net.dreamlu.mica.net.utils.timer;
}
