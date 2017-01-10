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

// Most of this file is based on Arduino sources written by Adafruit.
// Original sources are found at: https://github.com/adafruit/Adafruit-ST7735-Library
// The following is included as part of the original sources:

/***************************************************
  This is a library for the Adafruit 1.8" SPI display.
    This library works with the Adafruit 1.8" TFT Breakout w/SD card
      ----> http://www.adafruit.com/products/358
    The 1.8" TFT shield
      ----> https://www.adafruit.com/product/802
    The 1.44" TFT breakout
      ----> https://www.adafruit.com/product/2088
    as well as Adafruit raw 1.8" TFT display
      ----> http://www.adafruit.com/products/618

  Check out the links above for our tutorials and wiring diagrams
  These displays use SPI to communicate, 4 or 5 pins are required to
  interface (RST is optional)

  Adafruit invests time and resources providing this open source code,
  please support Adafruit and open-source hardware by purchasing
  products from Adafruit!

  Written by Limor Fried/Ladyada for Adafruit Industries.
  MIT license, all text above must be included in any redistribution
 ****************************************************/
package com.venaglia.roger.console.server.pi;

/**
 * Created by ed on 1/9/17.
 */
public interface ST7735R {

    int ST7735_NOP     = 0x00;
    int ST7735_SWRESET = 0x01;
    int ST7735_RDDID   = 0x04;
    int ST7735_RDDST   = 0x09;
    
    int ST7735_SLPIN   = 0x10;
    int ST7735_SLPOUT  = 0x11;
    int ST7735_PTLON   = 0x12;
    int ST7735_NORON   = 0x13;
    
    int ST7735_INVOFF  = 0x20;
    int ST7735_INVON   = 0x21;
    int ST7735_DISPOFF = 0x28;
    int ST7735_DISPON  = 0x29;
    int ST7735_CASET   = 0x2A;
    int ST7735_RASET   = 0x2B;
    int ST7735_RAMWR   = 0x2C;
    int ST7735_RAMRD   = 0x2E;
    
    int ST7735_PTLAR   = 0x30;
    int ST7735_COLMOD  = 0x3A;
    int ST7735_MADCTL  = 0x36;
    
    int ST7735_FRMCTR1 = 0xB1;
    int ST7735_FRMCTR2 = 0xB2;
    int ST7735_FRMCTR3 = 0xB3;
    int ST7735_INVCTR  = 0xB4;
    int ST7735_DISSET5 = 0xB6;
    
    int ST7735_PWCTR1  = 0xC0;
    int ST7735_PWCTR2  = 0xC1;
    int ST7735_PWCTR3  = 0xC2;
    int ST7735_PWCTR4  = 0xC3;
    int ST7735_PWCTR5  = 0xC4;
    int ST7735_VMCTR1  = 0xC5;
    
    int ST7735_RDID1   = 0xDA;
    int ST7735_RDID2   = 0xDB;
    int ST7735_RDID3   = 0xDC;
    int ST7735_RDID4   = 0xDD;
    
    int ST7735_PWCTR6  = 0xFC;
    
    int ST7735_GMCTRP1 = 0xE0;
    int ST7735_GMCTRN1 = 0xE1;

    int DELAY = 0x10000000;

    int[] INIT_SEQ = {                  // Init for 7735R
              21,                       // 21 commands in list:
              ST7735_SWRESET,   DELAY,  //  1: Software reset, 0 args, w/delay
                150,                    //     150 ms delay
              ST7735_SLPOUT ,   DELAY,  //  2: Out of sleep mode, 0 args, w/delay
                500,                    //     500 ms delay
              ST7735_FRMCTR1, 3      ,  //  3: Frame rate ctrl - normal mode, 3 args:
                0x01, 0x2C, 0x2D,       //     Rate = fosc/(1x2+40) * (LINE+2C+2D)
              ST7735_FRMCTR2, 3      ,  //  4: Frame rate control - idle mode, 3 args:
                0x01, 0x2C, 0x2D,       //     Rate = fosc/(1x2+40) * (LINE+2C+2D)
              ST7735_FRMCTR3, 6      ,  //  5: Frame rate ctrl - partial mode, 6 args:
                0x01, 0x2C, 0x2D,       //     Dot inversion mode
                0x01, 0x2C, 0x2D,       //     Line inversion mode
              ST7735_INVCTR , 1      ,  //  6: Display inversion ctrl, 1 arg, no delay:
                0x07,                   //     No inversion
              ST7735_PWCTR1 , 3      ,  //  7: Power control, 3 args, no delay:
                0xA2,
                0x02,                   //     -4.6V
                0x84,                   //     AUTO mode
              ST7735_PWCTR2 , 1      ,  //  8: Power control, 1 arg, no delay:
                0xC5,                   //     VGH25 = 2.4C VGSEL = -10 VGH = 3 * AVDD
              ST7735_PWCTR3 , 2      ,  //  9: Power control, 2 args, no delay:
                0x0A,                   //     Opamp current small
                0x00,                   //     Boost frequency
              ST7735_PWCTR4 , 2      ,  // 10: Power control, 2 args, no delay:
                0x8A,                   //     BCLK/2, Opamp current small & Medium low
                0x2A,
              ST7735_PWCTR5 , 2      ,  // 11: Power control, 2 args, no delay:
                0x8A, 0xEE,
              ST7735_VMCTR1 , 1      ,  // 12: Power control, 1 arg, no delay:
                0x0E,
              ST7735_INVOFF , 0      ,  // 13: Don't invert display, no args, no delay
              ST7735_MADCTL , 1      ,  // 14: Memory access control (directions), 1 arg:
    //            0xC8,                   //     row addr/col addr, bottom to top refresh
                0xA8,                   // rotate display counter-clockwise
              ST7735_COLMOD , 1      ,  // 15: set color mode, 1 arg, no delay:
    //            0x05,                   //     16-bit color
                0x08,                   //     18-bit color
              ST7735_CASET  , 4      ,  // 16: Column addr set, 4 args, no delay:
                0x00, 0x00,             //     XSTART = 0
                0x00, 0x7F,             //     XEND = 127
              ST7735_RASET  , 4      ,  // 17: Row addr set, 4 args, no delay:
                0x00, 0x00,             //     XSTART = 0
                0x00, 0x7F,             //     XEND = 127
              ST7735_GMCTRP1, 16      , // 18: Magical unicorn dust, 16 args, no delay:
                0x02, 0x1c, 0x07, 0x12,
                0x37, 0x32, 0x29, 0x2d,
                0x29, 0x25, 0x2B, 0x39,
                0x00, 0x01, 0x03, 0x10,
              ST7735_GMCTRN1, 16      , // 19: Sparkles and rainbows, 16 args, no delay:
                0x03, 0x1d, 0x07, 0x06,
                0x2E, 0x2C, 0x29, 0x2D,
                0x2E, 0x2E, 0x37, 0x3F,
                0x00, 0x00, 0x02, 0x10,
              ST7735_NORON  ,    DELAY, // 20: Normal display on, no args, w/delay
                10,                     //     10 ms delay
              ST7735_DISPON ,    DELAY, // 21: Main screen turn on, no args w/delay
                100
    };
}
