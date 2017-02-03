/*
 * Copyright 2016 - 2017 Ed Venaglia
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.venaglia.roger.autocomplete.reducers;

/**
 * Created by ed on 1/31/17.
 */
public class HardConsonantReducer extends AbstractReducer {

    {
        map("abe");
        map("ace");
        map("ade");
        map("afe");
        map("age");
        map("ake");
        map("ale");
        map("ame");
        map("ane");
        map("ape");
        map("are");
        map("ase");
        map("ate");
        map("ave");
        map("axe");
        map("aze");
        map("ebe");
        map("ece");
        map("ede");
        map("efe");
        map("ege");
        map("eke");
        map("ele");
        map("eme");
        map("ene");
        map("epe");
        map("ere");
        map("ese");
        map("ete");
        map("eve");
        map("ewe");
        map("exe");
        map("eze");
        map("eaze", "eze");
        map("ibe");
        map("ice");
        map("ide");
        map("ife");
        map("ike");
        map("ile");
        map("ime");
        map("ine");
        map("ipe");
        map("ire");
        map("ise");
        map("ite");
        map("ive");
        map("ixe");
        map("ize");
        map("obe");
        map("ode");
        map("oge");
        map("oke");
        map("ole");
        map("ome");
        map("one");
        map("ope");
        map("ore");
        map("ose");
        map("ote");
        map("ove");
        map("owe");
        map("oxe");
        map("oze");
        map("ube");
        map("uce");
        map("ude");
        map("ufe");
        map("uge");
        map("uke");
        map("ule");
        map("ume");
        map("une");
        map("upe");
        map("ure");
        map("use");
        map("ute");
        map("uve");
        map("uwe");
        map("uxe");
        map("uze");
        map("c", "k");
        map("ck", "k");
        map("ch", "k");
        map("dd", "d");
        map("ff", "f");
        map("g", "j");
        map("gg", "j");
        map("gh", "f");
        map("ll", "l");
        map("mm", "m");
        map("nn", "n");
        map("pf", "p");
        map("ph", "f");
        map("pp", "p");
        map("q", "k");
        map("qu", "k");
        map("rr", "r");
        map("ss", "s");
        map("tt", "t");
        map("vv", "v");
        map("x", "ks");
        map("xx", "ks");
        map("zz", "z");
        map("a", "");
        map("e", "");
        map("i", "");
        map("o", "");
        map("u", "");
        map("y", "");
    }
}
