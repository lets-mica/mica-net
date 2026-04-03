package net.dreamlu.mica.net.core.utils;

import java.util.ArrayList;
import java.util.List;

public class DecodeByteBufferListTest {

	public static void main(String[] args) {
		List<Integer> list = new ArrayList<>();
		list.add(4);
		list.add(55);
		list.clear();
		System.out.println(list.size());
		list.add(666);
		list.add(777);
		list.add(888);
		System.out.println(list.size());
		System.out.println(list);
		list.remove(0);
		System.out.println(list.size());
		list.remove(0);
		System.out.println(list.size());
		list.remove(0);
		System.out.println(list.size());
	}

}
