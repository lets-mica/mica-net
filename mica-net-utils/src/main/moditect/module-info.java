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
	exports org.tio.utils;
	exports org.tio.utils.buffer;
	exports org.tio.utils.cache;
	exports org.tio.utils.collection;
	exports org.tio.utils.compression;
	exports org.tio.utils.hutool;
	exports org.tio.utils.json;
	exports org.tio.utils.mica;
	exports org.tio.utils.page;
	exports org.tio.utils.prop;
	exports org.tio.utils.queue;
	exports org.tio.utils.thread;
	exports org.tio.utils.thread.pool;
	exports org.tio.utils.timer;
}
