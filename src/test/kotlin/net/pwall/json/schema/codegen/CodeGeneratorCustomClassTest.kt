/*
 * @(#) CodeGeneratorCustomClassTest.kt
 *
 * json-kotlin-schema-codegen  JSON Schema Code Generation
 * Copyright (c) 2020, 2021 Peter Wall
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.pwall.json.schema.codegen

import kotlin.test.Test
import kotlin.test.expect

import java.io.File
import java.io.StringWriter
import java.net.URI

import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.codegen.CodeGeneratorTestUtil.createHeader
import net.pwall.json.schema.codegen.CodeGeneratorTestUtil.dirs
import net.pwall.json.schema.codegen.CodeGeneratorTestUtil.outputCapture
import net.pwall.json.schema.parser.Parser
import net.pwall.json.schema.validation.FormatValidator
import net.pwall.json.schema.validation.PatternValidator

class CodeGeneratorCustomClassTest {

    @Test fun `should use specified custom class`() {
        val input = File("src/test/resources/test1")
        val codeGenerator = CodeGenerator()
        val stringWriter = StringWriter()
        codeGenerator.basePackageName = "com.example"
        codeGenerator.outputResolver = outputCapture(TargetFileName("Person", "kt", dirs + "person"), stringWriter)
        codeGenerator.addCustomClassByURI(URI("http://pwall.net/test/schema/utility#/\$defs/personId"),
                "com.example.person.PersonId")
        codeGenerator.addCustomClassByURI(URI("#/properties/name"), "com.example.person.PersonName")
        codeGenerator.generate(input)
        expect(createHeader("Person.kt") + expected) { stringWriter.toString() }
    }

    @Test fun `should use specified custom class in Java`() {
        val input = File("src/test/resources/test1")
        val codeGenerator = CodeGenerator(TargetLanguage.JAVA)
        val stringWriter = StringWriter()
        codeGenerator.basePackageName = "com.example"
        codeGenerator.outputResolver = outputCapture(TargetFileName("Person", "java", dirs + "person"), stringWriter)
        codeGenerator.addCustomClassByURI(URI("http://pwall.net/test/schema/utility#/${'$'}defs/personId"),
                "com.example.person.PersonId")
        codeGenerator.addCustomClassByURI(URI("http://pwall.net/test/schema/person#/properties/name"),
                "PersonName", "com.example.person")
        codeGenerator.generate(input)
        expect(createHeader("Person.java") + expectedJava) { stringWriter.toString() }
    }

    @Test fun `should use specified custom class for extension`() {
        val input = File("src/test/resources/test-custom-class.schema.json")
        val codeGenerator = CodeGenerator()
        val stringWriter = StringWriter()
        codeGenerator.basePackageName = "com.example"
        codeGenerator.outputResolver = outputCapture(TargetFileName("TestCustom", "kt", dirs), stringWriter)
        codeGenerator.addCustomClassByExtension("x-test", "money", "com.example.util.Money")
        codeGenerator.generate(input)
        expect(createHeader("TestCustom.kt") + expectedForExtension) { stringWriter.toString() }
    }

    @Test fun `should use specified custom class for extension in Java`() {
        val input = File("src/test/resources/test-custom-class.schema.json")
        val codeGenerator = CodeGenerator(TargetLanguage.JAVA)
        val stringWriter = StringWriter()
        codeGenerator.basePackageName = "com.example"
        codeGenerator.outputResolver = outputCapture(TargetFileName("TestCustom", "java", dirs), stringWriter)
        codeGenerator.addCustomClassByExtension("x-test", "money", "Money", "com.example.util")
        codeGenerator.generate(input)
        expect(createHeader("TestCustom.java") + expectedJavaForExtension) { stringWriter.toString() }
    }

    @Test fun `should use specified custom class for format`() {
        val input = File("src/test/resources/test-custom-class-format.schema.json")
        val parser = Parser()
        parser.nonstandardFormatHandler = { keyword ->
            if (keyword == "money")
                FormatValidator.DelegatingFormatChecker(keyword,
                        PatternValidator(null, JSONPointer.root, Regex("^[0-9]{1,16}\\.[0-9]{2}")))
            else
                null
        }
        val codeGenerator = CodeGenerator()
        codeGenerator.schemaParser = parser
        val stringWriter = StringWriter()
        codeGenerator.basePackageName = "com.example"
        codeGenerator.outputResolver = outputCapture(TargetFileName("TestCustom", "kt", dirs), stringWriter)
        codeGenerator.addCustomClassByFormat("money", "com.example.util.Money")
        codeGenerator.generate(input)
        expect(createHeader("TestCustom.kt") + expectedForExtension) { stringWriter.toString() }
    }

    @Test fun `should use specified custom class for format in Java`() {
        val input = File("src/test/resources/test-custom-class-format.schema.json")
        val parser = Parser()
        parser.nonstandardFormatHandler = { keyword ->
            if (keyword == "money")
                FormatValidator.DelegatingFormatChecker(keyword,
                        PatternValidator(null, JSONPointer.root, Regex("^[0-9]{1,16}\\.[0-9]{2}")))
            else
                null
        }
        val codeGenerator = CodeGenerator(TargetLanguage.JAVA)
        codeGenerator.schemaParser = parser
        val stringWriter = StringWriter()
        codeGenerator.basePackageName = "com.example"
        codeGenerator.outputResolver = outputCapture(TargetFileName("TestCustom", "java", dirs), stringWriter)
        codeGenerator.addCustomClassByFormat("money", ClassName("Money", "com.example.util"))
        codeGenerator.generate(input)
        expect(createHeader("TestCustom.java") + expectedJavaForExtension) { stringWriter.toString() }
    }

    companion object {

        const val expected =
"""package com.example.person

/**
 * A class to represent a person
 */
data class Person(
    /** Id of the person */
    val id: PersonId,
    /** Name of the person */
    val name: PersonName
)
"""

        const val expectedJava =
"""package com.example.person;

/**
 * A class to represent a person
 */
public class Person {

    private final PersonId id;
    private final PersonName name;

    public Person(
            PersonId id,
            PersonName name
    ) {
        if (id == null)
            throw new IllegalArgumentException("Must not be null - id");
        this.id = id;
        if (name == null)
            throw new IllegalArgumentException("Must not be null - name");
        this.name = name;
    }

    /**
     * Id of the person
     */
    public PersonId getId() {
        return id;
    }

    /**
     * Name of the person
     */
    public PersonName getName() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof Person))
            return false;
        Person typedOther = (Person)other;
        if (!id.equals(typedOther.id))
            return false;
        return name.equals(typedOther.name);
    }

    @Override
    public int hashCode() {
        int hash = id.hashCode();
        return hash ^ name.hashCode();
    }

}
"""

        const val expectedForExtension =
"""package com.example

import com.example.util.Money

/**
 * Test custom class.
 */
data class TestCustom(
    val aaa: Money,
    val bbb: Money? = null
)
"""

        const val expectedJavaForExtension =
"""package com.example;

import com.example.util.Money;

/**
 * Test custom class.
 */
public class TestCustom {

    private final Money aaa;
    private final Money bbb;

    public TestCustom(
            Money aaa,
            Money bbb
    ) {
        if (aaa == null)
            throw new IllegalArgumentException("Must not be null - aaa");
        this.aaa = aaa;
        this.bbb = bbb;
    }

    public Money getAaa() {
        return aaa;
    }

    public Money getBbb() {
        return bbb;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof TestCustom))
            return false;
        TestCustom typedOther = (TestCustom)other;
        if (!aaa.equals(typedOther.aaa))
            return false;
        return bbb == null ? typedOther.bbb == null : bbb.equals(typedOther.bbb);
    }

    @Override
    public int hashCode() {
        int hash = aaa.hashCode();
        return hash ^ (bbb != null ? bbb.hashCode() : 0);
    }

}
"""

    }

}
