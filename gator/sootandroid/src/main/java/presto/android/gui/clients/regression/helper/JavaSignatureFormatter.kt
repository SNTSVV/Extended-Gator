/*
 * JavaSignatureFormatter.kt - part of the GATOR project
 *
 * Copyright (c) 2019 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package presto.android.gui.clients.regression.helper

/**
 * Created by Fabrizio P.
 *
 */
object JavaSignatureFormatter {

    internal var PAR_SEPARATOR = ","

    class SignatureFormatterResult(
        internal val className: String,
        internal var methodName: String,
        internal var arguments: String,
        internal var returnValue: String
    )


    fun translateJavaLowLevelSignatureToSoot(signature: String): String {
        val res = processJavaLowLevelSignature(signature)

        return extractSootSignature(res)
    }

    private fun extractSootSignature(res: SignatureFormatterResult): String {
        //<com.teleca.jamendo.api.impl.JamendoGet2ApiImpl: com.teleca.jamendo.api.Album[] getAlbumsByTracksId(int[])>

        var sig = "<"

        sig += res.className

        sig += ": "

        sig += res.returnValue

        sig += " "

        sig += res.methodName

        sig += "("
        sig += res.arguments
        sig += ")>"

        return sig
    }

    private fun processJavaLowLevelSignature(sig: String): SignatureFormatterResult {

        val pos = sig.indexOf("(")
        val methodName = sig.substring(0, pos)

        val argS = sig.indexOf("(")
        val argE = sig.indexOf(")")
        val arguments = sig.substring(argS + 1, argE)

        val retVal = sig.substring(argE + 1)


        val mArgs = processArrayName(arguments)
        val mRet = processArrayName(retVal)

        val cPos = methodName.lastIndexOf('.')

        val mCLass = methodName.substring(0, cPos)
        val mName = methodName.substring(cPos + 1)

        return SignatureFormatterResult(mCLass, mName, mArgs, mRet)


    }

    private fun processArrayName(argumentName: String): String {

        var sig = ""

        var i = 0
        val len = argumentName.length
        var post = ""
        var p = 0
        while (i < len) {

            if (p > 0) {
                sig += PAR_SEPARATOR
            }

            val c = argumentName[i]
            if (c == 'L') {
                val end = argumentName.indexOf(';', i)
                val objType = argumentName.substring(i + 1, end)
                sig += objType.replace('/', '.')
                sig += post
                i = end
                post = ""
                p++
            } else if (c == '[') {
                post = "[]"
            } else {
                sig += translate(c)
                sig += post
                post = ""
                p++
            }


            i++
        }
        return sig


    }

    fun translate(c: Char): String {
        when (c) {
            'I' -> return "int"
            'B' -> return "byte"
            'D' -> return "double"
            'S' -> return "short"
            'J' -> return "long"
            'F' -> return "float"
            'C' -> return "char"
            'Z' -> return "boolean"
            'V' -> return "void"
        }
        throw IllegalArgumentException("Unknown ByteCode Type")
    }
    ///**
    //   * return a class name that follows the java type definition
    //   * if the given class is an array, even multidimensional
    //   */
    //  public static String getNormalizedClassName(Class c) {
    //    if ( c.isArray() )
    //      return processArrayName(c);
    //    else
    //      return c.getName();
    //  }

    //  private static String estractArguments(Class[] arguments) {
    //    String args = "";
    //    for ( int i = 0; i < arguments.length; i++)
    //      args = args + getNormalizedClassName(arguments[i]) + ",";
    //    if ( !args.equals("") )
    //      args = args.substring(0, args.length() - 1);
    //    return args;
    //  }
    //
    //  public static String estractMethodSignature(String targetClass,String methodName, Class returnType , Class[] arguments) {
    //    String args = estractArguments(arguments);
    //    return //getNormalizedClassName(returnType) + " " +
    //      targetClass+"."+methodName + "(" + args + ")";
    //  }
    //
    //  public static String estractConstructorSignature(String constuctorName, Class[] arguments) {
    //    String args = estractArguments(arguments);
    //    return constuctorName + ".new(" + args + ")";
    //  }

    fun getClassName(methodSignarure: String): String {
        return ""
    }
}
