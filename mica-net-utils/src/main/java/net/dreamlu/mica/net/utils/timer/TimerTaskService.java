/*
 * Copyright (c) 2019-2029, Dreamlu 卢春梦 (596392912@qq.com & dreamlu.net).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dreamlu.mica.net.utils.timer;

import java.util.function.Function;

/**
 * TimerTask service
 *
 * @author L.cm
 */
public interface TimerTaskService {

	/**
	 * 添加 ack 任务
	 *
	 * @param timerTask timerTask
	 * @param <T>      泛型
	 * @return TimerTask
	 */
	<T extends TimerTask> T add(T timerTask);

	/**
	 * 添加 ack 任务
	 *
	 * @param consumer timerTask consumer
	 * @param <T>      泛型
	 * @return TimerTask
	 */
	<T extends TimerTask> T addTask(Function<SystemTimer, T> consumer);

	/**
	 * 启动
	 */
	void start();

	/**
	 * 停止
	 */
	void stop();

}
