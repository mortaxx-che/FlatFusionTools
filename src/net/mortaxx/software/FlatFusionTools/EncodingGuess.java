package net.mortaxx.software.FlatFusionTools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;

/**
* Raten des Character-Encodings von Dateien.
* Achtung: Es wird die gesamte Datei gelesen.
* 
* Copyright by Thorsten Horn
* https://www.torsten-horn.de/techdocs/encoding.htm
*/

public class EncodingGuess

{

   /** Raten des Character-Encodings von Dateien im angegebenen Verzeichnis. */

/*   public static void main( String[] args ) throws IOException

   {

      String dir = ( args.length > 0 && args[0].trim().length() > 0 ) ? args[0] : ".";

      showGuessedEncodingsFromFilesInDir( dir );

   }*/

 

   /** Raten des Character-Encodings von Dateien im angegebenen Verzeichnis. */

   public static void showGuessedEncodingsFromFilesInDir( String dir, Locale loc) throws IOException

  {

      String[] files = (new File( dir )).list();

      if( files == null || files.length == 0 ) {

         System.out.println( "Fehler: Keine Datei in '" + dir + "' gefunden." );

         return;

      }

      Arrays.sort( files );

      for( String file : files ) {

         File datei = new File( dir + File.separator + file );

         if( datei.isFile() && !datei.getName().endsWith( ".class" ) ) {

            guessEncodingFromFile( datei, false, true, loc );

         }

      }

   }

 

   /** Raten des Character-Encodings von der angegebenen Datei. */

   public static String guessEncodingFromFile( File datei, boolean preferISO8859, boolean showMsg, Locale loc) throws IOException

   {

      String encoding = null;

      try( FileInputStream fis = new FileInputStream( datei ) ) {

         encoding = guessEncoding( fis, preferISO8859, showMsg, datei.length(), loc);

      }

      if( showMsg ) {

         try( BufferedReader isr = new BufferedReader( new InputStreamReader( new FileInputStream( datei ), encoding ) ) ) {

            System.out.println( "Datei " + datei + ", erste Zeile \"" + isr.readLine() + "\": \nEncoding = " + encoding + "\n" );

         }

      }

      return encoding;

   }

 

   /** Raten des Character-Encodings eines InputStreams. 

   public static String guessEncoding( InputStream is ) throws IOException

   {

      return guessEncoding( is, true, false );

   } */

 

   /** Raten des Character-Encodings eines InputStreams. */

   public static String guessEncoding( InputStream is, boolean preferISO8859, boolean showMsg, long filelength, Locale loc) throws IOException

   {

      final String[] boms = { "0000FEFF", "UTF-32BE", "FFFE0000", "UTF-32LE",

                              "EFBBBF", "UTF-8", "FEFF", "UTF-16BE", "FFFE", "UTF-16LE" };

      final String euroUtf16BE = "20AC";

      final String euroUtf16LE = "AC20";

      Long bytenumber = new Long(filelength);
      byte[] ba = new byte[bytenumber.intValue() * 2];

      int len, posArray = ba.length / 2, posStream = 0;

      int anzahlNull = 0, anzahlNonUtf8 = 0, anzahlUtf8 = 0, anzahlUtf16BE = 0, anzahlUtf16LE = 0;

      boolean exists809F = false, existsNonAscii = false;

      boolean ready = false;

      // Es wird in die obere Haelfte des Byte-Arrays ba geladen.

      // In die untere Haelfte wird das vorher geladene Byte-Array kopiert.

      // So koennen auch Zeichen korrekt untersucht werden, die ueber das Ende des Byte-Arrays hinausgehen:

      while( (len = is.read( ba, ba.length / 2, ba.length / 2 )) > 0 && !ready ) {

         if( showMsg && posStream == 0 ) {

            System.out.print( "Erste Zeichen in Hex: " );

            for( int i = ba.length / 2; i < ba.length / 2 + Math.min( len, 30 ); i++ ) {

               System.out.printf( "%02X ", Byte.valueOf( ba[i] ) );

            }

            System.out.println();

         }

         for( int i = ba.length / 2; i < ba.length / 2 + len; i++ ) {

            if( ba[i] == 0 ) { anzahlNull++; }

            if( ba[i] <  0 ) { existsNonAscii = true; }

            int bi = ba[i] & 0xFF;

            if( bi >= 0x80 && bi <= 0x9F ) { exists809F = true; }

         }

         // Es wird vorlaeufig nur bis 4 Bytes vor dem Byte-Array-Ende untersucht:

         int posArrayMax = Math.min( ba.length / 2 + len - 1, ba.length - 4 );

         for( ; posArray < posArrayMax && !ready; posArray++ ) {

            // Auf UTF-16 pruefen:

            boolean u16 = false;

            if( posArray % 2 == 0 ) {

               String hex2 = toHexString( ba, posArray, 2 , loc);

               if( (ba[posArray] == 0 && ba[posArray+1] != 0) || euroUtf16BE.equals( hex2 ) ) {

                  anzahlUtf16BE++;

                  posArray++;

                  u16 = true;

               } else if( (ba[posArray] != 0 && ba[posArray+1] == 0) || euroUtf16LE.equals( hex2 ) ) {

                  anzahlUtf16LE++;

                  posArray++;

                  u16 = true;

               }

            }

            if( !u16 && ba[posArray] <= 0 ) {

               // Nach BOM suchen:

               if( posStream == 0 && posArray == ba.length / 2 ) {

                  String hex = toHexString( ba, posArray, 4, loc);

                  for( int i = 0; i < boms.length; i+=2 ) {

                     int len2 = Math.min( hex.length(), boms[i].length() );

                     if( boms[i].equals( hex.substring( 0, len2 ) ) ) {

                        return boms[++i];

                     }

                  }

               }

               // Auf UTF-8 pruefen:

               int nu8b = anzahlUtf8Bytes( ba, posArray );

               if( nu8b <= 0 ) {

                  anzahlNonUtf8++;

               } else {

                  anzahlUtf8++;

                  posArray += nu8b - 1;

               }

            }

            ready = anzahlNonUtf8 + anzahlUtf8 + anzahlUtf16BE + anzahlUtf16LE > 50;

         }

         posStream += len;

         posArray  -= ba.length / 2;

         ba = Arrays.copyOf( Arrays.copyOfRange( ba, ba.length / 2, ba.length ), ba.length );

      }

      if( showMsg ) {

         System.out.println( "exists809F=" + exists809F + ", anzahlNonUtf8=" + anzahlNonUtf8 +

               ", anzahlUtf8=" + anzahlUtf8 + ", anzahlUtf16BE=" + anzahlUtf16BE + ", anzahlUtf16LE=" + anzahlUtf16LE );

      }

 

      if( !existsNonAscii && anzahlNull == 0 ) {

         return ( preferISO8859 ) ? "ISO-8859-1" : "US-ASCII";

      }

      if( anzahlUtf16LE > anzahlUtf16BE && anzahlUtf16LE > anzahlUtf8 && anzahlUtf16LE > anzahlNonUtf8 ) {

         return "UTF-16LE";

      }

      if( anzahlUtf16BE > anzahlUtf8 && anzahlUtf16BE > anzahlNonUtf8 ) {

         return "UTF-16BE";

      }

      if( anzahlUtf8 > 0 && anzahlNonUtf8 == 0 ) {

         return "UTF-8";

      }

      if( exists809F && Charset.availableCharsets().containsKey( "windows-1252" ) ) {

         return "windows-1252";

      }

      return ( existsNonAscii || preferISO8859 ) ? "ISO-8859-1" : "US-ASCII";

   }

 

   public static int anzahlUtf8Bytes( byte[] ba, int pos )

   {

      if( ba[pos] >= 0 ) { return 1; }

      int i1 = ba[pos+0] & 0xFF;

      if( i1 >= 0x80 && i1 <= 0xC1 ) { return 0; }

      if( i1 >= 0xF5 && i1 <= 0xFF ) { return 0; }

      int i2 = ba[pos+1] & 0xFF;

      if( i2 <  0x80 || i2 >  0xBF ) { return 0; }

      if( i1 >= 0xC2 && i1 <= 0xDF ) { return 2; }

      int i3 = ba[pos+2] & 0xFF;

      if( i3 <  0x80 || i3 >  0xBF ) { return 0; }

      if( i1 >= 0xE0 && i1 <= 0xEF ) { return 3; }

      int i4 = ba[pos+3] & 0xFF;

      if( i4 <  0x80 || i4 >  0xBF ) { return 0; }

      if( i1 >= 0xF0 && i1 <= 0xF4 ) { return 4; }

      return 0;

   }

 

   public static String toHexString( byte[] ba, int pos, int len, Locale loc)

   {

      StringBuilder hex = new StringBuilder();

      int posEnde = Math.min( pos + len, ba.length );

      for( int i = pos; i < posEnde; i++ ) {

         if( ba[i] >= 0 && ba[i] < 0x10 ) { hex.append( "0" ); }

         hex.append( Integer.toHexString( ba[i] & 0xFF ) );

      }

      return hex.toString().toUpperCase( loc );

   }

}