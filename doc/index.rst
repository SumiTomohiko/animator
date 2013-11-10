
animator
********

.. image:: icon.png

.. contents:: Table of contents

Overview
========

animator is an Android application to make stop motion movie.

.. image:: my_tablet_with_a_doll.png

Screenshots
===========

.. image:: screenshot-thumb.png
    :target: screenshot.png

Example movie
=============

One example movie made by this application has been uploaded to `YouTube`_.

.. _YouTube:
    http://www.youtube.com/playlist?list=PLsm28L9E7zoOZgiWsvdJkCIN5v866N1J7

Supported tablet
================

I tested this application with ASUS MeMO Pad HD7 (Android 3.2.2 with 7inch
screen).

Google play
===========

This application is available at `Google play`_.

.. _Google play: https://play.google.com/store/apps/details?id=jp.gr.java_conf.neko_daisuki.android.animator

How to use
==========

Install nexec client for Android
--------------------------------

This application uses `nexec`_ to generate a movie. Please install
`nexec client for Android`_ before using.

.. _nexec: http://neko-daisuki.ddo.jp/~SumiTomohiko/nexec/index.html
.. _nexec client for Android: https://play.google.com/store/apps/details?id=jp.gr.java_conf.neko_daisuki.android.nexec.client

Shot and "Make movie"
---------------------

Photos which you shot are added to the list at bottom. When you finished, select
"Make movie" menu to make a movie. When finished, you see the "Finished."
message.

Watch your movie
----------------

Your movie will be at /sdcard/.animator/project_name/movie.mp4. Please open this
with your filer.

Problems
========

Both of the current version of this application and `fsyscall`_/`nexec`_ are
**ULTRA** toy. These include many problems (which will be fixed in the future).

.. _fsyscall: http://neko-daisuki.ddo.jp/~SumiTomohiko/fsyscall/index.html

fsyscall is very slow.
----------------------

fsyscall is very slow. The example movie was generated from about 20 jpeg files.
Generating the movie took about one minute (A usual desktop machine can do it in
a few seconds).

I recommend you to try only with two or three pictures at first to know how slow
fsyscall is.

Anything else
=============

License
-------

This application is under `the MIT license`_.

.. _the MIT license:
    https://github.com/SumiTomohiko/animator/blob/master/COPYING.rst#mit-license

GitHub repository
-----------------

Source code of this application is hosted in `GitHub`_.

.. _GitHub: https://github.com/SumiTomohiko/animator

Author
------

The author of this is `Tomohiko Sumi`_.

.. _Tomohiko Sumi: http://neko-daisuki.ddo.jp/~SumiTomohiko/index.html

.. vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4 filetype=rst
