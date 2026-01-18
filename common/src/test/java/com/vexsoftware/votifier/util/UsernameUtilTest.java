package com.vexsoftware.votifier.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UsernameUtilTest {

    @Test
    public void testShort() {
        Assertions.assertFalse(UsernameUtil.isValid("a"));
        Assertions.assertFalse(UsernameUtil.isValid(""));
        Assertions.assertFalse(UsernameUtil.isValid("!"));
        Assertions.assertFalse(UsernameUtil.isValid("1"));
        Assertions.assertFalse(UsernameUtil.isValid("_"));
    }

    @Test
    public void testLong() {
        Assertions.assertFalse(UsernameUtil.isValid("17charslooooooong"));
        Assertions.assertFalse(UsernameUtil.isValid("loooooooooooooooooong"));
        Assertions.assertFalse(UsernameUtil.isValid("very_looooooooooooooooooong"));
    }

    @Test
    public void testValid() {
        Assertions.assertTrue(UsernameUtil.isValid("xz"));
        Assertions.assertTrue(UsernameUtil.isValid("16charsloooooong"));
        Assertions.assertTrue(UsernameUtil.isValid("cat"));
        Assertions.assertTrue(UsernameUtil.isValid("azurebytes"));
        Assertions.assertTrue(UsernameUtil.isValid("Herobrine"));
        Assertions.assertTrue(UsernameUtil.isValid("Steve"));
        Assertions.assertTrue(UsernameUtil.isValid("Notch"));
        Assertions.assertTrue(UsernameUtil.isValid("jeb_"));
        Assertions.assertTrue(UsernameUtil.isValid("123456"));
        Assertions.assertTrue(UsernameUtil.isValid("______"));
    }

    @Test
    public void testSpecialChars() {
        Assertions.assertFalse(UsernameUtil.isValid("azure!"));
        Assertions.assertFalse(UsernameUtil.isValid("azure?"));
        Assertions.assertFalse(UsernameUtil.isValid("azure-"));
        Assertions.assertFalse(UsernameUtil.isValid("azure+"));
        Assertions.assertFalse(UsernameUtil.isValid("azure#"));
    }

    @Test
    public void testNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> UsernameUtil.isValid(null));
    }
}
