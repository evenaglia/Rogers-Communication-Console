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

package com.venaglia.roger.console.server.impl;

import com.venaglia.roger.ui.FontLoader;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Created by ed on 1/27/17.
 */
public class TestImage {

    private final BufferedImage image;

    public TestImage(BufferedImage image) {
        this.image = image;
    }

    private TestImage(String b64) throws IOException {
        this(ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(b64))));
    }

    public BufferedImage getImage() {
        return image;
    }

    @SuppressWarnings("SpellCheckingInspection")
    public static TestImage getSolid(Color color) throws IOException {
        assert color != null;
        BufferedImage image = new BufferedImage(160, 128, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = (Graphics2D)image.getGraphics();
        g.setColor(color);
        g.fillRect(0,0,160,128);
        g.dispose();
        return new TestImage(image);
    }

    @SuppressWarnings("SpellCheckingInspection")
    public static TestImage getColorBars() throws IOException {
        return new TestImage(
                "iVBORw0KGgoAAAANSUhEUgAAAKAAAACACAMAAAC7vZIpAAABO1BMVEUCAgIKAAIE" +
                "BAQFBQUGBgYRCB0PChAQDAsPCxkPCxwRDBAKDwkKDhcPDgkLDhUPDgoODg4LDw4R" +
                "DgcODhAKDxINDw4NEAcKEBAPDw8IEgoMEQ1JAQIfDhRPAgoSExcMFhUUFBQTFRQM" +
                "GAwhEwADA/8DBP4WFhYBBf4AHyUbGwBoAR8AIx5GBX0fIAAAJy0WG6pfAORgAOdl" +
                "AOFhAedkAeVnAPlQE4oAOgALKaEKK5ILLpYOLpH/AQnxBgL9AwQuQkv4Af//AP//" +
                "Af/8A/35BP/6BP32BvvzB//2B/jkFNzaGNnfFuB3SLyAQ9+AROJdo/hdpflTxUut" +
                "qvsD/AAB+vwF+v0C+/8C/PoC/PsA/fsA//QQ/9ns6u///A39/gP9/wT/6+r/6+3/" +
                "8Pz49/X89vj/9vb/+fn7+v/7/fph/+z1AAAAAWJLR0QAiAUdSAAAAAlwSFlzAAAL" +
                "EwAACxMBAJqcGAAAAAd0SU1FB+AMHQgbAW0xedUAAAAmaVRYdENvbW1lbnQAAAAA" +
                "AENyZWF0ZWQgd2l0aCBHSU1QIG9uIGEgTWFjleRfWwAAAUZJREFUeNrt0tVOA0EA" +
                "heHBHZbF3aW4w+LuDsVbHPr+T0CyW7INnAmEpFz9//XJzJfJmEdd7F4Vvz5X3Z7J" +
                "TtZUGwuLqvkunQEIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQI" +
                "ECBAgAABAgQIECBAgAABAgQIECDA/wQO6wYjqo6DQ9X+tGzpWHXU16RqONWZHl1l" +
                "taplRT7Kbo2sN6q6Ka9TFd7pjOVlSx1V86oE7six0y//w2W+HFdZ/hpAgAABphvY" +
                "qStxXbfi2ynty+t+W5vbqe0Vh5WF66EL1VWOBj7ozJxuxvO855evPb0m/AbGJlKb" +
                "HAkrCO9szFPlZmQHJVe1rX5t9Tozay/xZmtqdNxWkfNTmck+gd1BljVAgAABAgQI" +
                "ECBAgAABAgQIECBAgH8CvqcHaAJf1u+AH2W5R6gIl+R/AAAAAElFTkSuQmCC");
    }

    @SuppressWarnings("SpellCheckingInspection")
    public static TestImage getCheckerboard() throws IOException {
        return new TestImage(
                "iVBORw0KGgoAAAANSUhEUgAAAKAAAACACAQAAAAmaqqQAAAAAmJLR0QA/4ePzL8A" +
                "AAAJcEhZcwAACxMAAAsTAQCanBgAAAAHdElNRQfgDB4FLyceezNiAAAAJmlUWHRD" +
                "b21tZW50AAAAAABDcmVhdGVkIHdpdGggR0lNUCBvbiBhIE1hY5XkX1sAAALeSURB" +
                "VHja7Z2xbtRAFEXvjCeJEBLdgvgfaEIBEjV/RwUFVajo+Q4qlvwASmR7qBCd75U8" +
                "WmXDuW2ejsdnnRnpjTUu6rIJSnTUS40hlaDmWjeDSHtHVEV2BYEIRCACEUgQiEAE" +
                "IpAgEIEIRCBBIAIR+P+kfDIt2aKuzwHoqa5tzZU+mopZT/TO/uZFP/Td3Zh+64Op" +
                "6Vr0xdZMer91ndX2tLuafM0L/TQ1i6RLy5FWU3Ovpq96a0lFi63purA/xLo5oub3" +
                "DYrdN5i0qNodiEk92IHwnCt1TUN2V+aAc6m7TU4bMQ8sSjZnSrTJsw4Q8+9p3s5F" +
                "QLljEWEVRiACCQIRiEAEEgQiEIEIJAhEIAIfT9pkClZNtrNbVHSUJynoEU+WU1T1" +
                "ypKkakld3XJWzZuc5huYS/iYelKxpKSlX7WoR2NypCm6t8vtlv4pH/eka51pGTPz" +
                "LEFN3fl3gkAEIhCBBIEIRCACCQIRiEAEEgQi8AHFNlRLYLlLei5PmgNSiRqzNwFp" +
                "CkbUd7d426izVZJmaPJmc4+aqiUgrRrx/nfTfJpzY+oQxSPTh1Bm5kAWEQQikCAQ" +
                "gQhEIEEgAhGIQIJABCLw8WRYQ/V20LHXvqFa1PU6IC2DWrzbfUwrsKtGF/H93x4I" +
                "7MGpHVJVs6TkHJGuJRjR9k/axrTG0+e0BH/PnlJHaicaEXMgiwgCEYhAgkAEIhCB" +
                "BIEIRCACCQIReEZpR1vS7Ucfi1YddLRVJSDNlrOq6VtAutcxEJB80HKL0w52IF23" +
                "geQqT1JEOtiKSc8saZUCUg1GtG5ymv8fLtHZzhpyEEnKqcNIew9rGTYH9pPOPP3B" +
                "XItFhFUYgQhEIEEgAhGIQIJABCIQgQSBCDyjDPmyoST9GvKSeQ8/3/cmICl62/oE" +
                "L5n7DzoWe5m/t5SQMs4Y0rqb00a8iK3olnKSTkjay2EOZBFBIAIRSBCIQAQikCAQ" +
                "gQhEIEEgAs8ofwCxCbD+Nzru2QAAAABJRU5ErkJggg==");
    }

    @SuppressWarnings("SpellCheckingInspection")
    public static TestImage getAspectRatioGrid() throws IOException {
        return new TestImage(
                "iVBORw0KGgoAAAANSUhEUgAAAKAAAACABAMAAAB+TX8oAAAAMFBMVEUAAAAXFxcl" +
                "JSUzMzNCQkJUVFRfX19xcXF/f3+MjIydnZ2srKy9vb3Ly8vb29vn5+frgVTdAAAA" +
                "AWJLR0QAiAUdSAAAAAlwSFlzAAALEwAACxMBAJqcGAAAAAd0SU1FB+EBCAQEDm3F" +
                "IwcAAAAmaVRYdENvbW1lbnQAAAAAAENyZWF0ZWQgd2l0aCBHSU1QIG9uIGEgTWFj" +
                "leRfWwAAB4hJREFUaN7tmltvE0cUx2d9tynSLpfY5qGyDSROnxwcJQ68OI4aSHjo" +
                "BhAUepHdQMKlqHagOKVK5VApdVRQzaUEhyJAlJKgqq1UiRapRUgVLfBUVWrV9Au0" +
                "fIM+dmZ29jLj9e6Om1ZC7Ug58Vl7fvbOnJn97zkL6vUri3XYniBzD5krd2n/I93/" +
                "DPuPkF3C/izy8cvfiA8A8IrQgCIyHch4Ypofxn4K2bzmu7OMLyM7gowfGeDFgAoy" +
                "vdhPacAkBmY1P4J9mfExu6D5Tw2w+DeBMgUU2mcfzsRbBwobq49munSgMNclAilz" +
                "tmVgNQf7d9xQgfFD+D+Iyi0Ch/CgAf+0AvT0iQoQRHrM4tBtF4d7Y6R/eCs0Y2Mv" +
                "1YDavtk9Nja2bwaasQfInMH+RfTye2SmsP8JNAd0f+99rT/0gSQlNR8E4pIktfVD" +
                "s2oaGqkPmbUjyJZ0v4DshOZH9f6+EUmNQNKK5mO4KrH+RiIhmY9hwdC/hKii4UCH" +
                "CdB3u348vSmRTh9cvGwC9MqG/hG1k9r8Igt0bVvMam/3LuQagEFjfx/8bM14QMgz" +
                "wMDNOPX+hrrIAPuN78PxE4rMAQrYeR4wTah303FJ9+8HrhR1YIICvt4FGprwXI0C" +
                "Vqh3N9BzgmdFBw7LwKxtHjEAPfRnIiBAA0MG4FAKmLekrAP99IeC9CRBoCB5u1C4" +
                "VsRwETRrL8ZEHOidghQUGeAUeUWOr6xWT39dhe2PqemmPCC8MfkEfejqZPUDuv8K" +
                "EADsKSubQ34nsGhCXiYXJT/7CwPsATKGt0QrIGj7goxhIMb099IHegnQXQPW7TgB" +
                "uvPMLDNxWCLA/TY84Co2iUPzleLN2gFBu+lKgSF6lhpsWQFO2PLwL0HAHHW0xuwW" +
                "AWW38cXsgWj/jyhxYdhtUmqUkPa8sh+OOuChnxhRB9PwJdSoCsqO7ck6AcJRjGgy" +
                "SZ/TdHq77q8YSKfTmdEDjnjAfWw7/Hh6ix6xoZ1pUC4f0aZF+HS8DP25a86AoHoG" +
                "ff7QT9qBd8bL6BST6hnuwRd6T6dDHgjiU3aPqbHdqUqRSWVeknlFOZxzCgSXiHJQ" +
                "Fke4ommbfRcAWDPfT7RN3jFwmGibvpsicJ8sGORcuL50USTqK+iYp+wsSM75Zh/P" +
                "xZoJzpJzIO5lp2CFPAcw5wAYFDmAIQfAQQ4e3g0bgB76PqXCA0TdmPsUqA9PIKn3" +
                "MzKnod6TuYBboT68g/TiV0QvQn3Y1qXpwW1Q7YlcwJAkrSlperFTQvrQR41hlIuH" +
                "5JbNpJT4gLCfDbDICSzYAIUsJ3CTDdDR1cTYojbAECcPdrAGJnmBHrkBSIXNKC8Q" +
                "VBigJHnjWA8ikxFq3MBpD7oREl9D/SOCBKrVMwtQ6c3+ifTevdlr3MC33v8VdV0i" +
                "elHbHPDvbgMyNzDrkrUb8MYkBncYwgu75Sy7UtzAdksgd1zDyLYEBkVuYMgSyL1Q" +
                "nlKgqAE7WgAGVtNAqAd3QpHX/SWSeoe38AP94+dR1wvIQL2I9OF7UOSVf0HmyttN" +
                "+zWdfv/8D7DniYeo/5SiDw3X5VZOeb38f9j8o0tv2TcH93JvXy1ssN3W+pD/EtBv" +
                "rQ/5r3pla33IDzxlrQ8nuIEVaynSyy1F8sssloKiNTDAu1RCyy040/+2JOa87zEV" +
                "7fRtRQev3gTW+lAKcs6JIFnqQ/iCbzVvrlat9WELN4+W+hD+7eLhocyR3f1yiC+s" +
                "HaQIeAZxwAGQaxCbJjE8B38fBfxpFn+MAF3jS8eNwMytTa7ehZQC9Dk/580kEdSx" +
                "MCD01Ps14DBO+QtDKSWz5PxehaSq1uELitCXJ8B2NVPzZgdfMs2fwZmRXeqo7yH6" +
                "8LqW+b2LRF5mv9MdZzfOH/Z8p0XlZawPj+p76rNlnD887zCq53D+8Ki+ha4sl+lc" +
                "F05TeWIOBUlYmYQcvRIBpT5GlaRuyeGyQ0DBGLdr2bALKmlnv5MrS9Ak7YwqA9SV" +
                "2JVVEuMlZ/sCAnYziXGhwq4lBHQwihFRARaYS4wrawa0VxCCeXFhPVv+GCRAr936" +
                "6yGbgavIzDxzZY+qBZoH1jzvjwTYUORqrJopxYGC9bxMy8rvUTYcI/Bl+k7pGa2+" +
                "/GrBgveCVl9+he4fYtN7Ib2+3NlclXR2afXldaLtKWuFwvFm4d1WaVooDLGLImkA" +
                "CifNozF6CuhAJhyigLmRGKWKrXUzYvg6AE2LrWvZPHiFLgcPN26NRw7Q9eUimym3" +
                "LlgPv0sPyaqTMlOwHqHDCdCqGq0buqS++vBNg9aaPyayJfUIrbiZ4mYUNBb9g4/n" +
                "com4lEgM1r8lCs4I9KXoLY2uFFZMgJ6UkEgP3hlIx0XTxxKM/XdAfSgakv8+JBVJ" +
                "fRmZpACNBwU6yAtKPRn66EEKrb4MRaHxjLE+1L/iKhJ5pL6MzOesP4n8h+ilWl+G" +
                "+vC+viIVfbhHHcW23fhrYppetHj4RNeDrkm1v3dc+SfNkBOexhd67sdjCgfJCZ8L" +
                "kFDxXojDydn4casP8Lg/RP3XzOliyTU0v3gp1/ojRq5M/falHSKlD2uaPmztIahi" +
                "c8H5X3iQbHkfxlvuxwX/AnYpmtz4OdX5AAAAAElFTkSuQmCC");
    }

    @SuppressWarnings("SpellCheckingInspection")
    public static TestImage getSkinTones() throws IOException {
        return new TestImage(
                "iVBORw0KGgoAAAANSUhEUgAAAKAAAACACAMAAAC7vZIpAAAC/VBMVEUJBgYbCQwA" +
                "C1QPDiAFEjsKFw0AEHYREzAlEBUzDQsCF04rEgsiEicIIQwdGxk2FB0oGRpOFQ9C" +
                "GBUWGplfEw8AJnQjJCIIMBEnJhpMHRM4IiIBJLgmKCUcJ1IcLSQVKWI+JBs3IzsD" +
                "LXIyKCd0Fw4kKUgpKygwKxhWIB9PIxwgMhlNJCJ2GRctLD6DFxItLixfICVtHxgN" +
                "Pxk2MSswMjU6MSU6MDAxMzBRKjNJLieRGxJiKB1EMDEaNIddLB9qKhIAPY4LPIEO" +
                "OZhANymTIB6gHRZdLys3OTw+ODJBNzc4OjdeLFdIOy0jM/ItRDYQTyA9Pz1GPFAw" +
                "QWiMLSWvJBwAS55AQj9YPDSCMiOOMBh1OCNxOSkkSIIWS5FtOzFGRUOFNTYlR8Bb" +
                "RkhtQUUaXi5nSCsAWqu1MjaKRBqhPxwnTfp/Rza+L21cUEyGRy97ST5pTkSgQSWh" +
                "M7uuPUrAMo0PaLYyWustY6X/MDR6WDCST0OgTTtNXoiaPvmWVCtjXXSZRM+CV1eP" +
                "VjyMV0OIWEqYWCCNSs03c0udRemoUVLFP7qyVSUac8GeWUJ7YGb/NoZ/YlmOZy2K" +
                "ZlKHZHFya2ykYFSeY0n/PLOaZVCWZlfeQvowfL9ebN3/Sl4dgfFMf7Vlep/Gai2u" +
                "bVdRiGd2epL5TeSldzywcWWmdWWpdl7Ha3OgemmbcsiAg3+vfyyzbdrLdzW/e167" +
                "eoQAuUC5gGmghYSpf6Z9kIO0g2l0j7ObhLS5gnUFumehi26Pjo0Arvu3h3ayiXT/" +
                "b5ivinsAu5lmmcwqrMG6jHHCkTKPlKyqjKzZiUG3k0P0cuf/dcXFkInAlIC8lofE" +
                "lnvGlou5n47fm1XUojPDno/SmKHInoibrKbwjujRo4bOpo/hpG4A2fzMp5bGqKO8" +
                "uTiatdfesjrNraDSrpwN80Ud82vltZTUuLfYuajmv0XQzDsl9pwX8/HKw7+1x9/y" +
                "tOw19MvozW7q15nx0u3281j690n8+YH7+Zj798T9+a749O/799m2IQ+cAAAAAWJL" +
                "R0QAiAUdSAAAAAlwSFlzAAALEwAACxMBAJqcGAAAAAd0SU1FB+EBGxQ5IIVxPXcA" +
                "AAAmaVRYdENvbW1lbnQAAAAAAENyZWF0ZWQgd2l0aCBHSU1QIG9uIGEgTWFjleRf" +
                "WwAAIABJREFUeNq1nH9c03ee58Ooxy8Vk8V04KBBAgw0SKRNEzCYxl1MTNrpAJYf" +
                "EgiMVtTLQeJ5DFP1ypTE7XkFL6XKJZedASlXEzN1Oz3YpsvVSS7HRjtzZxKYxuxK" +
                "J5yFucrejd5td/d+zDzu/f58v98k3xBs7497t1LUPuqzr/fvz/fzDecW2Pu3vrEl" +
                "/tX343ZFpmWZDIx8oUwsFleI8UuyFVFfivBvxviUxb8hxmH+tCdwvs/Cof/t9zcH" +
                "TIKTSaVSsVhWQSArKghqCmgqXyrgR/CnfQT2/kfpDP74j97fzK7Q1s8GTFJPsq+2" +
                "dp9QgiLSEjJ8RUUMYaqCRfAXP8HI+RcJ+zcs+49x+yvG/jtjfxu334E92iAghcjX" +
                "1DY21tTkM4QEkP5BCIuS+GjhKFDqCwX4xz8C++eM/TNizM/wt370x2h/CvZTxv6c" +
                "sk8//fQOFQJpPSzpmbt09lJjTVm1SCKWxZ0rpvio6GMJSBiZb+KKcv7kB2j/iNh3" +
                "WfYPGfsD2v4BZd8ilpOzNTMzcwsHbGdTGkKpNTZ/4+HZ/TVl+ZViGR2DjIaMa5Nc" +
                "zKeU4ycAaRf/IAH43e9+I8JvJRFu2bIZoDV29+y9D8HD1fmgYBIf7WsKi6Ug5duU" +
                "kOT8CWPf//4P4vb9ZPtD2g4QKy0tFYAVo5WX7927l8+XpQJqtWJrbPHSPSJg9T4h" +
                "BRUnZGVxESuPN/DxOf8Z7L9S9ndofw/2dwn7n5T9L7T/Q+x/U/Z7tN/9Ll2SaLUS" +
                "U2Tl+o2HNxoBsLZSmgCMC0kFYcLDbClTABN8fx+3JyBuINwAKO0Praxc+vAe8lXn" +
                "q6QVFeziFy/URYyEFWkYnwj4tXy//30SoJrNp9H4Y6srdz88CxFYVl3bLmYDplTq" +
                "TYwphZzf/OY3/wXtvxH7H8R++9vf/gXYwoJjdPQnJEB/+tM/JfZXVGX8T2j/ntiP" +
                "f/zjS1I2n8QdW11dmb8EAoKHextS2pw4VcY0/k3UagT8Dc1Hs/3217/+9c9//vOf" +
                "/OQnmN1MSpMC+a/RsIr/K7B/CfbjH//whz98JRlQo1VbQT8EBL6a6l494Aklknj8" +
                "idO4O52C/P9PgFptfxD0W1mcv95bU9Nx7ohILMTaImTQxCkKkq9FyfWQDfhv0f4d" +
                "2H+g7C+ILUw6Rgf7WqHI/AFdCqGd/OifgFFd8Z+C/WOwsydPnnylkQWo8Udiscii" +
                "d3567Pz580fyJfWVtdXVtZWFQmG8z6WKyOomKVm8K40J8ce309vTKYb/kcSw0AQO" +
                "XlxcBL7ZWQA80lhWeeTQK2CHDu2vhYJdQRcc1lhTlAaQjsEiPqetqa2trQm+NMFX" +
                "Ym3kG01b4vsmdRPUYvq3yXca+lv8mZqVxP2+xcXg3dn52elp0K9m/8lXiB06eagR" +
                "xwYSieIkEcUVFZtkCeEDwE2MgqO+bWtj4JOsLQGcZCBgcDESmp6+Pj09BnyvnGQI" +
                "D8Fkk0+NhHTHo2VMmhn4qSFYtDlgEmobI2+cKZmYzWeKRhZnQb/rY0S/V2g8ysn7" +
                "a7gVlJsTA2J86KpI9nHSwNr2TYwtWcK0KYTq/mAkshiZRQEhAve/8koSHwKWiZL4" +
                "KEQGrSI5Q5KyOMWraQVEuDR4NCGVHFSP80ajscXQovPq2NjVMzUUGuVkGjBfmIAj" +
                "zuaL01Xq9AoSjk1pn4AXnxGswBeJLc5756+Ona8h6Xvyxo0bJ+OANbWFyQIWaTRS" +
                "9lhYxGfh8Ys4T4q8JwK2JfI44eBodG0VgtAVd/DJGx/eu4eENGA1GQ5xgJXINFCR" +
                "7t693sAXp1Ww6BvEYE8SLxuO/gVWgmjcyzHocpGg1zUNGUIJeO9eMmB1NU9MFlFJ" +
                "xWHr4sri3RuXzp7dVyRMdfMmLv76RIlXRiqR2TOCNbq8uhqLRLyzrrEzNWUE8OwN" +
                "CpARsLoSFAREqdUP7fD6pcaTUH0q08zWKQr2fLNEpqK0iamPyYQaqDCxNejCMegi" +
                "IGBZNR2CwHf25CEEbGysri6D8VUm4ZvmF1dW7l46e+PDRkydQj5TaPiphJyvLYGp" +
                "IdjWxjiZ7WNNdHkZHbzo9bjGjpTly6kUPnvjxlkCuH9/IwhYJpLKJA1OL/AtXrrx" +
                "8N4R5CvjpSx4/MQGyqH16/laEVkx2JaIQZpR7Uf9IuDgedcECFgpOkQXmZOv0ALW" +
                "EECxtN3pjaB/P3yISxUO3ZXidAMrS8EeVmY0qTcEJJMlbcmRmNziltdgiImEgG96" +
                "rKMsv7D+UHKRJnwAWF0olF9Fvoj36tmziAd8+YXpJmp2FvckE6qbjl3uh68buzPt" +
                "37YNKdwfXF6OwJTlW8Qxq7qaK2loZPHtJzBl3ELuOeddEBqKOfUr+fm1DRuTJDHy" +
                "tyUR0pCq3jfuhha99mPqnoR7mdxIzD3xCYsIGI6txoKBSMgFQ0JHbX6hRFp9KIUP" +
                "vcnl6uxeMJh2zh0hhPm17UUbh+r4ERIHyRg2mqehZv/ZD+/enb9qSgJsSh6/SIVO" +
                "FtB0P7K6Cn045PG4JgZqayslMpnqEGP7aT7odNxau9vpmva4XGPnOkC/6trOdr4w" +
                "DliR3E8II4fBIj6mvtVU1+xvvL64OH/V2NOUtsclJQeVwb5oLBaKBD2e0OwECFjb" +
                "AICyahZfGbqTKzc7nc7rtgnXxNjYwEBHR6e+oWjzzY5qdVQC9yQSuam2Zv/+I3cj" +
                "d69fNWo2zDFqNainZpfo8WgwEMQ5+mdez9j5jlq5FE+PGhppPKIf4asFPpttwjYx" +
                "NvL666+fOqWrFyZ34pRDBj6jIK1hPJVFALj/euTu/PWrenWybmq15qV2jTplD9b2" +
                "e6LRiGcW1hBPJIQCihBQKqvH/w6ewFGA+fk6O+hnG5sYGRkAvoun6gFi0904OUl6" +
                "EpBo6mqUMIQSnlMlgk5tev0DsGtvDbTDdpnUg+3haCQI+TF2NbjihQiUEw9LJZL6" +
                "aib6MP5qzX6/32UbGRgZAf0uXjz1GthLUqGYjUj/LHmi7mGXmbamyhogvB6CKLza" +
                "Ua9VEzrNiQ9+QdkH164BopbRUdPvCYSDofnp6WmXL4gCqiTUMbVU1rCvuqaGSFjW" +
                "cc4bividLqAbGAP3Xrx5+4svvvjss9uvPc+vSKKrSMqSzUb+Jk0ZEB6BYnD96vle" +
                "DckJzQSg/QoMvqKKejWjomY8HAx6p6FCO/0hz1jtC5AiUvqEWiJtEFVz91V3jLn8" +
                "kZVIyOeaQMAB0O/2F3/9BSH87OYJYcphYeI8My0g7GyV+D99HQBhNVOpcXObIHQE" +
                "EDT84NppBa2g+pgn7AXCiWmnN+iZ6Kh9QU7RkYNCiUzIy6/1haAKrSKfi8TfxYs3" +
                "P/vir9EI4s3X6EpTsbHUcFLHAmoM0FSDhpcW56/D8nOkAbLD+AsG8FeUk986raKn" +
                "GHc4GFmEEWF62ulK1BjGtD2un9l77z58iHxOiEASfrcpPJrw9u3XqJk1cVLDZ/7i" +
                "pFvhoJaIEDAEgFfPn+9Qaw9/gICfgxFCAtjRoCZjdABSJBT0znq8XlKk5RIpOUjH" +
                "L1JJ//215XDY63X6rON2G+E7depmnA8JIQ5vnkjpdnw6CNO5mBS+nt7qI/Mh9PHY" +
                "+TMq9cgHyPfll+tffv4r4uFrb6GEao1a4gpDkY4E571ezywAQgRqJfQxsFqisd6P" +
                "xWJr0aBv4h0OZ7t+YGDg1KlTF78AwL+JK0gR8tnHC3zqyBoBk70bbxz9ZhvyEQnP" +
                "dGjeAgE//3J9nQB+QAN2aDQamcYTjZEp1TMLfOc6alVS5mGTVHrYDTPYympseWn1" +
                "8vBwdnahDfg6T6CD/wYt4eObbz/HTwWkFKTHY2YOZZpGj3lsehY2cMjjsTNn9NeI" +
                "gHHAa+ji00cb1NZzFg/sSSuxkA/a6/TEQAepgYgokak1vlgsEAhGYtGAd3h4b96+" +
                "EkPnqU7p25AgQPfVV18hIhHw9i/fO8FPSWHqByc+o7CbrtoyDYCwP+L0dKaDAfzy" +
                "S9rDqOCZdunKpZUVTyAGCQBzNC2gjD7ol2iCYaiPkWAwEva4m2defPFF1QsvKJ6T" +
                "YgYj3le//woBPyOAv3xbmLafcOhlOHUxV497PB4vRhYC7qcAwUgIXiMuPnNGpTGN" +
                "r0Q+nF4KhUgFGUMB44/BYAmFCTYI2s56XabteXUvvnhMrhCKD98mIUgDfhEHfH6D" +
                "hBRg+iMD9XggGMbUhAWDAQTCzz9PCHj6aJVaY56duBp6NHd9DPhAwA4VdhCSvrlO" +
                "WEJDQc+sZ3YCsnd7VnZenaleKBb/EakxyYC3IQbfe/uP+OwTanonaUpvWqsnANV3" +
                "dtYDO+6Z/W+RMvg58iUBijS2QDjs8qwsHcm32EfOgYBSGU3Y79L5YHCen7sMeOaJ" +
                "CeOf5dXzvlMCS+fh25+RJMEYpAo18CFgygkm9VNOfJzCCS9pbjG5PDDezc5Chz1/" +
                "puY09rlfUDWQ4iNJYpgNwyoX9NhOd1y+YumEKUFNHndC/Vm+H3i0FF1/+KC4zjxi" +
                "NtsMnTqdiM+Hlf05iEE6jRkHE8DnNz5sYgDjA3zSqSQo6PHChOd1Xj1/tOZofFBI" +
                "4jsqnw4vr+Gq7rfZTCvWWnk7lEAq/paX79+PPVp1TwkEezo7DTbzgFwuIgdHEtnb" +
                "nxFCukjTAr73mnBDCtNJomWPytSRkLrf5QtEwzDjuWxjR46WvfUBA8jwnT5aNhCI" +
                "xVZXY1CGfS6zUVVrNLXLsErL+kMAuPwoGhouP9Bdx9UZzOZOeT0+cUL6E+DjL6hR" +
                "4Qua7+Z7F08wzxGFLE9zWIdVREQC2KSxeTzhgMfldF4FwOqOax98wNLvzNGjHbPB" +
                "2Bosm8GAz+ez6+TZ7ap2bH7aHv/aMrj+0fqjO83bL8+p5IaBTnkJzQcJREkY50MB" +
                "L158Xszn79zJLxJuKDOJ42bCSP90lysQDkQDHTABQ3KWZR+9do3QxfU7WjYWBAev" +
                "xZbDAZ/faTZW7pLW79wq2CvVupfXwJZXHs0MW+fG67lysHoJHhpRBeilm0iY8O/N" +
                "mxdBQGE77HvWBr4wLSAlW8Lb6voJqDCLcwNmu50A8k5fY/CQ7/TRmg5PbBU4oE0E" +
                "fG6nWc/bpd6yR6AslVgRD35rZma43f3IyuMCYYNYKpaJaUQxEn5G8yHg2xdPCesB" +
                "DxtXZSE/FZDxKysWC1//2dXa/FrDgN0MgNXZGaIzb1F2mvCVdcxGERA8HPQ57XZj" +
                "XcYWQWlL88HiBvD76vrqWnRlpsEypzCZCkA/CkzMFHFCGOeD6V9kdHq9V88fady/" +
                "v1aY1sUsRNCyvupnR6obDx2Rd547Bx2Ml1HY0HmGsJ05c+b00doyVziGDl6NQKQ6" +
                "7W8cz8rsbj1YunWraiS8tr6+urocDNgHDBauXDGiqJelmOTw27dvI95txLt4qtMM" +
                "8p1vpBZAkTBtDNJVhiy88LdVMX61pvFQDU9+dEBXm8/lFqo17R1gR8HkDVBhQECY" +
                "pGIhb8ANfHnF3X3KUsEWqX28PbD+cDUWDOMO36nrlMutu2QbTHri5u24fhMgn/N8" +
                "IwEsq+YW8ROTNSfxiIYtocbZ2e/tPSLX66vGOsrK9pWIyQLXUCeqb1Cr26fD92MY" +
                "gMvRsNdnNx6vK+4b7C4VFJdE1oPrj5ah9mAJmBgxQIEW7RJuBJRJpK/dJPnxugta" +
                "echrO9JIHz5UburiJESNxNqYL9JXqqp0titTLdvLhWp8kgnLCZj2GEypy7E1qHWQ" +
                "wi6X5fjx5qHBvgOlgnLn6vra2qPfrYdg1vBAjxsw6NqlElk6kxRJT0zgOB6J4Kxx" +
                "jsLLr+QVsgG16ZuxWlNT3dhYq8hrbx4ezszLFmqQ77BG0wTbuzMcjcaWiYEj7XpV" +
                "XdfgIDhYULe6CgG4vvxoqd44YYMFydCpkqbH42us7hCkE8w7IZ/P6zpHTrq4lYVC" +
                "fnoFU7yslhqq95+v5eor60R1Il52+VB3d7G65eDBcq3GG74fpfDAwx6XWa86NjjY" +
                "pxQIiu2rkL8P15fcekuBbsA80NmpSo8nNLkjWIygSHk8fpglbbp8wONW1RexzvxJ" +
                "L9YyA4M2CVDCaz93aWVO727Xq1QiXvfo6CgEWfdga2m5Oxi+T+igGEOk2Y16/YXR" +
                "vlaIQFFodT209uhRfW7Be3adrnMzvsNWQodHnsAGBvOYLp/Lq1ShfBWbK5hk0irb" +
                "G+88/LDXWmWaEmR2jToco4OtBwRTyoPvhO8j3xq2CwS0AeCQAwTMzLPA+rtifzTH" +
                "5b5w+71as6GzHY9o2NrJNP1uzC8AhFT3IhzEqsGgV1XWN1Skm6jTG1+h3/fOh729" +
                "++qaW4qLHY6FB44+AMwpHQ5GiXhUF4EcsRmNxqmpbkFmtsIfWV2PTYCzajs++6XF" +
                "7K6XImDSdTOxtN/qp+hQvhAsqjYzwdOr6qVY/4TfEFCj3pW3b1/tvuzs5rrs7OJR" +
                "xwMAVB4QZG53Ah8mAgJGg8GA66rRYFnpL8/M5lpCMQjB8AAIeO32e+NWE1GMkQ5M" +
                "Y/VHSZ+GXRTwgliIzAazQQd8DUL24UdFMmC6x8HNeft6a3szsvK4XG7L4OjC5GAr" +
                "5Ol2XRgm+bV18A80M1gKIEfeuDA01GKUc43uEDSXWMAzUvDa2+/lK+o1bO/2u2FL" +
                "ATgGDxZBFE+n01dVFbIuL/ATjJu6uKn50321HflZeZXcSlXz4OBQX6vyQGlxljNC" +
                "AgjmmPUYHhp5nJYpx2ifsllncPrh93C+1jccs5oUJ4SSxE1CDYh3PwrFE9kihA5T" +
                "1wzxqy+oKhEW4a0LVnokKZi+Eh77tLfjSO+ffXp83z798e5bw92tB3bnZOpRJJyx" +
                "SImASdBjtzoeTLYqi/V2XygCo4NnwmyU1+XlZZdINYxzhehbwItC0IaCQYoO8MC5" +
                "igLerp18sYS+vFDxxCRhefqle3dre+/97T19vlyna5lpKd4NfIX4uAbCLwaFGiLQ" +
                "6/E4bdZRiE5ludziB/go8umhLmXnnjhMjjFlMn5ugc4fAfgowHl8ATxoh8Sl8HLx" +
                "gt82zk7qdtQTsjhVRnW/d+Vc7+yL+/L39RqHHY4+GFQys9yrwEcA78PQHPRglbAP" +
                "YXpv5xp8waVIwDUCAQ8CZm7Xacj6zs9QyV/oNIRWYpFgxOcj0k2MDBgg+BRVuZxt" +
                "O54C27FtG78o7Vkwh3UbIWl/h7Xuem/vmLEyX2687FgAkbZwssZXKUBsw2vwB5Iq" +
                "63NfEORs5wJgKBp2mXVQ1wv3DPa1thRqNf0lDVVyudvuXgL/R4J+lxOLHskMnaIE" +
                "8J569ntgzz4LiDvTEnKamprSPTLUqE2wGE/DJKjrtVweXXC0lm4vGMdjyIcw62Go" +
                "QxxiDvvm5m7NHNPjAxpfxOeZMCpUorq8oYWFPuX2EqvFoneb5Xa/e2l9zYdVz3yu" +
                "k+ApiHpPAdyrrxJEJJQUbebipJsTzAUKLS7GAa/9nNlw7jLptFkqNwLiMEqKBfSB" +
                "oNc5Mwm/2dWsM9p8oZDHZpBXqUSiPQDYerDZbTYY3Wa/+RObPbS+OG22DZyD+ZDg" +
                "YeztePZVYrSGz2zjb6Zg/DFwcgyaPJ5gKDhrm7DZnReUf7g7M1vujsCQjxFIegFU" +
                "FJ93xjEIBjB2N/BNgIOrVKrjx4YGh5QCybhvadWU6/YNuG0TpuO1FsMLOp1BZ8TU" +
                "AL6nvvfqm2++SQEC31PPvLwtzRNFTvwhcFviH+QLAOLJqN/ndEYit/Y2cLlG6GSk" +
                "g2KRBtBYyDfnGIT8aW09UPyGHZbjERRQ9YbVfeFWf/HWY4/8Prdp3O2zeT75RDHW" +
                "MT4ulxtIYcnl8zk7vvfqux/95UfvvomEFODT4OT0MdjUFr8hRZ+wYpJAEEIl8boi" +
                "q7FHw3Y9NAoABDgkxLUjAgIOTj54DC3mgKDZAvFv1itUesu4e27q4+a87KXxoN/p" +
                "Mijsgdde+8TwST9nTg8VS8HL3SWW7dwG+n300SMgpADBxS+37azYrA4mjtHjLj4G" +
                "i3E0CokKDXZlaqjrsh1r2SptALgW9fmmAHABAA8KMhWGEbOR5oPWcnCP0b+05I5+" +
                "wlO0T3g+WVpSCBtOmBUFBbmwAEgkkB+vvvuXaO8yLn755Zf5FcJNAZtSMhkAPUFQ" +
                "0BOEhL3jcChboBAjGe1i8HDQ55sZHHSggADIkxuMIKBx3O2fWnjQpyy29FsGXOOd" +
                "L+gaRiZ89naTSd85YsouFOJDFMm2Z9MBSvjCzbJ44zFrP1EwDP09tvYxJGWxyk56" +
                "La6amMw4FTiv9PVBBCp3l+ZsyS7Q6xUKo8Xt809CXCqLVX7/2pLVZCpRjOjGszLq" +
                "DOMjr5sqqO5XtAMA33z3o48SMQgh+LK6aMM9pCRANp8aAPEIF4erNQi0voM8ox+C" +
                "kNCt4Jeoz+N0D7VSfFuzsnlVhA8AZ2CwKM3kWqwQc2ZrAdc9PmKyzplOjJx4nn4G" +
                "Jd6BBfDNdzGNv8dk8dNNEv6G+/PJdXADYDgaxu0ttvrx48eTyjw9ZAkdhGQahj7i" +
                "u9KtVEKT3rolAwH1Frvb7w/NXejavTVPbjUZrf5xv3vcPf6aa9zafqJQ9BI9HopR" +
                "QSB889UEHwBWCFPfMSniJMo0+xwdAKHMRAngyoPHDx50b1fY/XFCGAah6Xt8vrnh" +
                "GWjSGRnZAIgJ4vP5Q/6lK3nZCkCb+2R8bu6dwiyhySTiVZ3gFTLzPwJ+71W6jWAK" +
                "o4BPN/E33lzmsG+WJQN6EoDA92Aok2dBCYl7YfsNEz5feO6K1cjLRj6FEfSDXwrN" +
                "TbXU6aC/tRdare+8CNNKVlWJqL1EUSiOj9c7nkJAIh8FuAMB05WZlNtv8cGLAAZx" +
                "iFteBgUfP3BkZkMlXCJBiN0ujPuib27S0XXFDrWXcTACTjoGS/UWs0Vl7edkZGzP" +
                "yqiXFmbkFibGf5nkaWpKiOu3Y8fTT2vx6Xa6LE74NrndkU4SJvvv0p3Hjx+Plmfr" +
                "x/1LkVW6WAdhrMOEhZJnlBcUVKmQz+cHwrnRhb7SPAVMXe1bgG9Pc79U1lYvBMcm" +
                "ACt2AOGzxMi4tWPHtqdlO4Xxu9/xR3Yc6vlmvIHQMraRXhwmC3p0OfIxBOFgcZZq" +
                "HGZSSkAsMsA353jgaFXWyQuIfk4IQHD6lGOhtTQrOzsvK2vLFg382KuFhSl5O8Hb" +
                "b0BIiwd428AqdtJrEzuL25pY7m2Ld2ZwMRZCmNOX70PlhUKYVTUOzYSsS6vrURTQ" +
                "d2sQ1pHSPG5Bld5od0KG+ILBiGNySKkE7TIyODkCQeaWLZla9ukMuT/47WeeeQZz" +
                "4xmCx+Hs5PDpvSn1QQ77SSJztQgVREB08f2ZyQeTSgC0+CNMlQEPe0GwIaiCAhF4" +
                "GDMYAzAauwUCHjiIb8JsUUIZP5CTuUVNnbiR/YT8IDcIvwOIcbydnJ1pn7tz2DdV" +
                "4xdW2zAGPQQQ/r41tOBQFmcrYCalkhhyBPYRnz8y1yUoFsl5BVCinQgYiQTAwwcO" +
                "KDMBUAl9EGv2XqlQKAVraGinTEPsue+gffvb24COw+EL0wHyOax704mCo1ZbAwh4" +
                "H50cWxpyDB4ozobROETxrccCAeCDiLxyeaKTW0ADQp30maHRUYBbWx0LOEkc2JqR" +
                "u6tEpDcaLXa8l4K10g1mhT5oan+pvqSksKSk3Wh6wyTdMLLyk1zcxrqoBYABTxj9" +
                "C4DLePpXzAVAysXrq9FAIABrxp3JC3MjnfICBYYg4EV8BtUQLKEEUNA3+RgBlTkc" +
                "qOMKiwXpAA4y3R8Kwb+MB4Mh+Knf7Zy/Gwrd9buP8TcefaS71kg9TQTA+wCHaXKr" +
                "9WBOnVznDlF9eH2NPBuJxCYn+7om5HLIYbMdHB5xG+TNQw4lKMhBQFy2YBbLKSzJ" +
                "yq6HMu52z/tCWClDNCDZ4gnn4qJ33uV1S1NfwopnceKSMvO4EypxAI+xcEOKhW6N" +
                "O3UGd2SJ7iNBENAXueOYbD04IIcpEJsI6idXACCeQQBgTh+sK7DuC4ozstuX/FYT" +
                "DBJOF4aunyFcCYUIrX/++qUjaLWF/E0VTALEidrNAOLNxdW5mWG3xR5aiVB8y+FA" +
                "MBiKgEKtAgJoIXwjcpW+eWi09QCEHSdji/LCcPcBGHUyd0Gfdi9ZwcdulxPnHRox" +
                "FHJ5XXhbz3muhjpCrykTbt7qWBc/KEAMQljfVj92DA4Nw160Qk1aayQEox+TdVQH" +
                "e4gRojPkGZHL9cZjgwBYilUwQzB8RXBgd04xZ1d9e3tJSb9eZ7HbcDN2urBkOqmz" +
                "QafTZTNXxy94FVZ8HSCVyBofBRjDFe6OY2GytfQNAFxZwSqIT5dg53OMLgweLJVj" +
                "Drv9wKcDPsvloUGofdlZuVndg4PdW3NyIF2ewzrC4ezi4qmrwTBiG7GRY0vzOTwB" +
                "MY8M6KrLyqgzaq6QdQKcCtgT93YPPkoM4LgKhB9jRyttwGlmBUbqVXzG4AvdGR1d" +
                "6D5YWllQAB6GlY7wma0AuHs7jDctDkhhjEWOdfgKAeRkF3BhsdMZDAY8mQGT4xED" +
                "mJyLlg0LQfyEuiItYLwkwjCD4wooiA8M72Cwl+ZBmVnB1RgnGSgykwCoFJRW8gr0" +
                "drdtpFOnN5otZvsQLPkiHo/XNfmAAELHuzVM+Lbk8ngKPZgctjvYD+QKrkJeqVBU" +
                "VfFKCoXUW0+pj9w5m70A0e+jAXFFX7mgVH4rk8u1+MklrBjmcGgOACdLBYICBLRT" +
                "fGZw3OWpYpgeCgq6sIsIIBSzObRtgb0A4qEK6HklUJ5pE4IxXZhflPz+xiaAJEv6" +
                "AwgYiFKnCCszw8f0XIXVH8I6AzlMATqGEBDLNLoKG4Xd5rLdMhkwLpux2mTmZuXG" +
                "ATkZubkUD3n2TjqymHmfDfelNC/ZbXYTPaEgPrtcXZ+7NTysghUDZ34UEIoMAE4q" +
                "SwV7EBDwIP4sNrvN7pyZ6moGrykUxy5sxVjMzs6g+WT9ErGEHFtLJWLq2Sz7JbYn" +
                "DAtpAUmvIwLega7f1z1sxW4bi8DvBH1BGKZHD5QKqgAQDDotcbBtCnc4TzTmAAAG" +
                "s0lEQVTOPQVVVVW3rpiALjd31y4akC9kxi3qle3Ei9vM+9ob3wPkpHs3A53c7wtQ" +
                "gOjjjwehJwx2K4fdCAg57AsFERAmlT2QxGAkgRHQOrTwAEKviidqmewqz4Xgl6qF" +
                "muc4O/nS55KnVgYy5b38dAeYKYA9dJnx+ehpAQQcHIWxBM9Rj+P5B16iCAbD/kmo" +
                "yAKVggAq9Fg5RmxQZUZHAZDH2zv0oE+5V6rWkgFQqt3wuEmc8vJV+neOWTfRk2Ts" +
                "Ubs9NOAyVJmFx49Jh60zOkMRzOEgQE5CRd6jAEDYmPQ6A16PAevC9ruVl13ueNyn" +
                "LNfKNjP2q03MG4DsN/M3xGCyl3HcogBjU4OT1GCXsx1KIYylXm8QovACFDyVjvAp" +
                "oPiOQBGE6nv8IPDl8XgteECzOSDr1TBx0ivR3zCLezAIqV4cW5vqw0NKEDBnC9eI" +
                "XRRcHPZ53DPFxwdoQPSwxYLPtORVx64U8nh1Fx5M9ilbWJ9ZsWEvoT8dIvEG26aA" +
                "PRvfyukP4lJCHhveacUTGJhLtmZk6+xup5fEpwdW9hGDnAt8qCB414KP3JpbLky1" +
                "FIoAcKivteXrPUy/6Rl3cUVRmhjsSfOy1TEf8FHPXSNDSuTbnZOZwdPjWOwFdecm" +
                "J7uaR7AkF1AC4uG4QnUBE75V0KCeWujbHBBLjIypMsl5UpS61z0BMEAB4rww11Va" +
                "SviyCmC0dzlhYfKOwspeqtfBSgx1EAERT3FhdBQBlbvVkw4ElG5OKBMzbhYnvY2a" +
                "/KLnk5MEYhD47lMja2S4pbh8e0ZWtgqGTpfb45mdGcWVfU8nKogpAvLpVVUNow6c" +
                "YQCweBJKe9+TXCxLeYcyfSXc/MU/E0ZgdJke+iPON2Yu7OFV6i3gYZfL5bmCjyZK" +
                "9xA+hc6I00lVVUHXqIOcWStLc4bwbHOv9ImAso2V+htncRMsTbgUg4TkAaV/anSw" +
                "uxj6BXgYAV3oxxzYlyACFTo9uLdKJIIJZhRGW9iTSnOADwr15gomifgkwg0vXTH5" +
                "rHYHgqgfru6x5Vj0lgOnuzqcWMgtA9dc1+6tKozAKmzFhA8AEQsL0u6cbvzmiYDx" +
                "j8r5f1KQeX1I4yZFkBx9oIRTEFvK0jooJzbC55zDqUpewMMcUYF7q4Cwak8rPjTB" +
                "Ram8FZNZ8vVlWpz6iT5FFU8C7GFa3jEEpO7GkL3zDjlm4cl1FODc1NBQd0tdJQBS" +
                "JkKrqhIA325MeHUrePiJgLK0OZL6cQGczV447afaHOEjEg5Bp9uTz5UbLUA4Q1WT" +
                "0jxoI4AIEz4FKNIIsF7mZErU3fA/VCzTap+gYEVF8svGiY8A+UbzoMlDFiYCiNc7" +
                "IqHLt67AciM34lQ/CuUEx5scPPslRgBFmiZtz0zmXolaq+6CJKdbsfaJTq5gAaZ+" +
                "5MJmL5+qrRiCaygf5eJoJHhrqlkHSxlMfRdGFx48wGoi2B4H5FWJ6jVNLUMXhvsl" +
                "5D2TqfJy6tJC/EN8xKxOnJwj7FqdZu3sSSVsGocmskauotKA0bnR0e7mTswS29Dg" +
                "woOFPvIELJch5IkKgQ9cDyMM+USNW9Ye+iOatJtIKGOpuBGQnM3Qr6ylEvYg4DK5" +
                "4ELukUSj96EQ9nUfx7lvYnDQQZ6AlSIgw9egaTo8RHZhUFDTMjTUItGqtZtOM+Lk" +
                "MshKFdZ7YcxbYWzCHgoQFIxRkMvRYHgGAPu6BgbM5omhpCdgNGCDRt3UpOkbxTVA" +
                "oj3cMgr/BwKZesMHcbFdnMBj1qfUj63gMAJSiMzY39PvDNI5gj6Gch0M3xnE7noc" +
                "CEcuA17rAfoJGNlypeQNwcMtg9iGtVrpINb1rr3apAhkBWFCwookBYvSuJiB60mC" +
                "hH/0e+gYRPliEQScG0LA7o7OAYO+RUnGrxyarx6vPbZ0dXW3wNiTAzl8eAgn3IN7" +
                "afk2CULWwMW4mHWxB1xMoBg4BrGnx4Sz6toyVWZQQNjihhzoWVVnp65TVS7IyaH1" +
                "E4ka9rZ0dROvt17ZuxdKDIQgmWjUaf2bvpuIxckejr/8xwJMwhxHQBJ8UciQIJp/" +
                "cgEh9ryA97blostXynMEmcinaelupYJSuXtvTxO++NnU1CLIKZdoNwnBxKfWxT/q" +
                "SpwyDcaTpCedtfW4yLSPVZDEXzgcDAamyAxaTAHK9XshVZR7eKJmCg8nBNCMektQ" +
                "vbd/pV+d+lFw6ZpJmo/VY71VwtkgIGUeGjAKfNEoAIa9gT93LMQBdbpKcjdGIGrp" +
                "G8Srj9CAd38rk3qUgW2u9QKTwRs+749OlnilTv00KdbnMv1fs6GXHQ8lo5wAAAAA" +
                "SUVORK5CYII="
        );
    }

    private static final FontLoader FONT_LOADER = new FontLoader();

    public static TestImage getNumber(int num) throws IOException {
        assert num >= 0 && num < 8;
        BufferedImage image = new BufferedImage(160, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D)image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setFont(getHeavyFont().deriveFont(72.0f));
        Color c;
        switch (num) {
            case 0: c = new Color(32 , 32 , 32 ); break;
            case 1: c = new Color(0  , 32 , 255); break;
            case 2: c = new Color(0  , 192, 64 ); break;
            case 3: c = new Color(0  , 208, 255); break;
            case 4: c = new Color(192, 0  , 0  ); break;
            case 5: c = new Color(192, 0  , 255); break;
            case 6: c = new Color(255, 208, 0  ); break;
            case 7: c = new Color(208, 208, 208); break;
            default: throw new AssertionError("Bad value for 'num': " + num);
        }
        g.setColor(c);
        int w = 42, h = 48;
        g.drawString(String.valueOf(num), (160-w)/2, (128+h)/2);
        g.drawImage(renderLabel(String.format("0x%02x", 1 << num)), 0, 110, 160, 16, null);
        g.dispose();
        return new TestImage(image);
    }

    private static final int[] MASK_GRAY = {0xFFFFFF,0xFFFFFF,0xFFFFFF,0xFFFFFF,0xFFFFFF,0xFFFFFF,0xFFFFFF,0xFFFFFF,0xFFFFFF,9};
    private static final int[] MASK_HRGB = {0xFF0000,0x00FF00,0x0000FF,0xFF0000,0x00FF00,0x0000FF,0xFF0000,0x00FF00,0x0000FF,3};
    private static final int[] MASK_HBGR = {0x0000FF,0x00FF00,0xFF0000,0x0000FF,0x00FF00,0xFF0000,0x0000FF,0x00FF00,0xFF0000,3};
    private static final int[] MASK_VRGB = {0xFF0000,0xFF0000,0xFF0000,0x00FF00,0x00FF00,0x00FF00,0x0000FF,0x0000FF,0x0000FF,3};
    private static final int[] MASK_VBGR = {0x0000FF,0x0000FF,0x0000FF,0x00FF00,0x00FF00,0x00FF00,0xFF0000,0xFF0000,0xFF0000,3};

    static {
        for (int[] mask : new int[][]{MASK_GRAY,MASK_HRGB,MASK_HBGR,MASK_VRGB,MASK_VBGR}) {
            for (int i = 0; i < 9; i++) {
                mask[i] |= 0xFF000000;
            }
        }
    }

    protected static BufferedImage renderLabel(String label) {
        BufferedImage image = new BufferedImage(160 * 3, 16 * 3, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D)image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setFont(FONT_LOADER.get().deriveFont(18.0f * 3));
        Rectangle2D bounds = g.getFontMetrics().getStringBounds(label, g);
        float w = (float)bounds.getWidth();
        g.setColor(Color.WHITE);
        g.drawString(label, (160*3-w)/2.0f, 15 * 3);
        g.dispose();
        BufferedImage result = new BufferedImage(160, 16, BufferedImage.TYPE_4BYTE_ABGR);
        int[] buffer = new int[9];
        int[] mask = MASK_VBGR;
        float pels = mask[9];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 160; x++) {
                image.getRGB(x * 3, y * 3, 3, 3, buffer, 0, 3);
                int a___ = 0, _r__ = 0, __g_ = 0, ___b = 0;
                for (int i = 0; i < 9; i++) {
                    int argb = buffer[i] & mask[i];
                    a___ += argb >> 24 & 0xFF;
                    _r__ += argb >> 16 & 0xFF;
                    __g_ += argb >>  8 & 0xFF;
                    ___b += argb & 0xFF;
                }
                a___ = Math.round(a___ / 9.0f) & 0xFF;
                _r__ = Math.round(_r__ / pels) & 0xFF;
                __g_ = Math.round(__g_ / pels) & 0xFF;
                ___b = Math.round(___b / pels) & 0xFF;
                result.setRGB(x, y, a___ << 24 | _r__ << 16 | __g_ << 8 | ___b);
            }
        }
        return result;
    }

    private static Font font;

    private static Font getHeavyFont() {
        if (font == null) {
            try {
                font = Font.createFont(Font.TRUETYPE_FONT, TestImage.class.getClassLoader().getResourceAsStream("WorkSans-Regular.ttf"));
            } catch (FontFormatException | IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return font;
    }

}
