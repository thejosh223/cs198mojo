#!/usr/bin/python

#########################################################################
#
# Author : Choopan RATTANAPOKA, Jie Yang, Arno Bakker
#
# Description : Main ABC [Yet Another Bittorrent Client] python script.
#               you can run from source code by using
#               >python abc.py
#               need Python, WxPython in order to run from source code.
#
# see LICENSE.txt for license information
#########################################################################

# Arno: M2Crypto overrides the method for https:// in the
# standard Python libraries. This causes msnlib to fail and makes Tribler
# freakout when "http://www.tribler.org/version" is redirected to
# "https://www.tribler.org/version/" (which happened during our website
# changeover) Until M2Crypto 0.16 is patched I'll restore the method to the
# original, as follows.
#
# This must be done in the first python file that is started.
#


import os,sys
import urllib
original_open_https = urllib.URLopener.open_https
import M2Crypto
urllib.URLopener.open_https = original_open_https

# Arno, 2008-03-21: see what happens when we disable this locale thing. Gives
# errors on Vista in "Regional and Language Settings Options" different from 
# "English[United Kingdom]" 
#import locale
import signal
import commands
import pickle

try:
    import wxversion
    wxversion.select('2.8')
except:
    pass
import wx
from wx import xrc
#import hotshot

from threading import Thread, Event,currentThread,enumerate
from time import time, ctime, sleep
from traceback import print_exc, print_stack
from cStringIO import StringIO
import urllib
import webbrowser

if (sys.platform == 'win32'):
        from Tribler.Main.Dialogs.regdialog import RegCheckDialog

from Tribler.Main.vwxGUI.MainFrame import MainFrame
from Tribler.Main.Utility.utility import Utility
from Tribler.Main.Utility.constants import * #IGNORE:W0611
import Tribler.Main.vwxGUI.font as font
from Tribler.Main.vwxGUI.GuiUtility import GUIUtility
import Tribler.Main.vwxGUI.updateXRC as updateXRC
from Tribler.Main.vwxGUI.TasteHeart import set_tasteheart_bitmaps
from Tribler.Main.vwxGUI.perfBar import set_perfBar_bitmaps
from Tribler.Main.vwxGUI.MainMenuBar import MainMenuBar
from Tribler.Main.vwxGUI.font import *
from Tribler.Main.Dialogs.GUITaskQueue import GUITaskQueue
from Tribler.Main.Dialogs.systray import ABCTaskBarIcon 
from Tribler.Main.notification import init as notification_init
from Tribler.Category.Category import Category
from Tribler.Subscriptions.rss_client import TorrentFeedThread
from Tribler.Video.VideoPlayer import VideoPlayer,return_feasible_playback_modes,PLAYBACKMODE_INTERNAL
from Tribler.Video.VideoServer import VideoHTTPServer
from Tribler.Web2.util.update import Web2Updater
from Tribler.Policies.RateManager import UserDefinedMaxAlwaysOtherwiseEquallyDividedRateManager
from Tribler.Utilities.Instance2Instance import *
from Tribler.Main.globals import DefaultDownloadStartupConfig,get_default_dscfg_filename

from Tribler.Core.API import *
from Tribler.Core.Utilities.utilities import show_permid

I2I_LISTENPORT = 57891
VIDEOHTTP_LISTENPORT = 6878

DEBUG = True
ALLOW_MULTIPLE = False
        

##############################################################
#
# Class : ABCApp
#
# Main ABC application class that contains ABCFrame Object
#
##############################################################
class ABCApp(wx.App):
    def __init__(self, x, params, single_instance_checker, installdir):
        self.params = params
        self.single_instance_checker = single_instance_checker
        self.installdir = installdir
        self.error = None
            
        wx.App.__init__(self, x)
        
        
    def OnInit(self):
        try:
            self.utility = Utility(self.installdir)
            self.utility.app = self

            self.postinitstarted = False
            """
            Hanging self.OnIdle to the onidle event doesnot work under linux (ubuntu). The images in xrc files
            will not load in any but the filespanel.
            """
            #self.Bind(wx.EVT_IDLE, self.OnIdle)
            
        
            # Set locale to determine localisation
            #locale.setlocale(locale.LC_ALL, '')

            sys.stdout.write('Client Starting Up.\n')
            sys.stdout.write('Build: ' + self.utility.lang.get('build') + '\n')

            bm = wx.Bitmap(os.path.join(self.utility.getPath(),'Tribler','Images','splash.jpg'),wx.BITMAP_TYPE_JPEG)
            #s = wx.MINIMIZE_BOX | wx.MAXIMIZE_BOX | wx.RESIZE_BORDER | wx.SYSTEM_MENU | wx.CAPTION | wx.CLOSE_BOX | wx.CLIP_CHILDREN
            #s = wx.SIMPLE_BORDER|wx.FRAME_NO_TASKBAR|wx.FRAME_FLOAT_ON_PARENT
            self.splash = wx.SplashScreen(bm, wx.SPLASH_CENTRE_ON_SCREEN|wx.SPLASH_TIMEOUT, 1000, None, -1)
            
            # Arno: Do heavy startup on GUI thread after splash screen has been
            # painted.
            self.splash.Show()
            "Replacement for self.Bind(wx.EVT_IDLE, self.OnIdle)"
            wx.CallAfter(self.PostInit)    
            return True
            
        except Exception,e:
            print_exc()
            self.error = e
            self.onError()
            return False

    def OnIdle(self,event=None):
        if not self.postinitstarted:
            self.postinitstarted = True
            wx.CallAfter(self.PostInit)
            # Arno: On Linux I sometimes have to move the mouse into the splash
            # for the rest of Tribler to start. H4x0r
            if event is not None:
                event.RequestMore(True)
                event.Skip()


    def PostInit(self):
        try:
            # On Linux: allow painting of splash screen first.
            wx.Yield()
            
            # Initialise fonts
            font.init()

            #tribler_init(self.utility.getConfigPath(),self.utility.getPath(),self.db_exception_handler)
            
            self.utility.postAppInit(os.path.join(self.installdir,'Tribler','Images','tribler.ico'))
            
            # H4x0r a bit
            set_tasteheart_bitmaps(self.utility.getPath())
            set_perfBar_bitmaps(self.utility.getPath())

            cat = Category.getInstance(self.utility.getPath())
            cat.init_from_main(self.utility)
            
            # Put it here so an error is shown in the startup-error popup
            # Start server for instance2instance communication
            self.i2is = Instance2InstanceServer(I2I_LISTENPORT,self.i2icallback) 
            self.i2is.start()
            
            self.videoplayer = VideoPlayer.getInstance()
            self.videoplayer.register(self.utility)
            # Start HTTP server for serving video to player widget
            self.videoserv = VideoHTTPServer.getInstance(VIDEOHTTP_LISTENPORT) # create
            self.videoserv.background_serve()
            self.videoserv.register(self.videoserver_error_callback,self.videoserver_set_status_callback)

            notification_init( self.utility )

            #
            # Read and create GUI from .xrc files
            #
            #self.frame = ABCFrame(-1, self.params, self.utility)
            self.guiUtility = GUIUtility.getInstance(self.utility, self.params)
            updateXRC.main([os.path.join(self.utility.getPath(),'Tribler','Main','vwxGUI')])
            self.res = xrc.XmlResource(os.path.join(self.utility.getPath(),'Tribler', 'Main','vwxGUI','MyFrame.xrc'))
            self.guiUtility.xrcResource = self.res
            self.frame = self.res.LoadFrame(None, "MyFrame")
            self.guiUtility.frame = self.frame
            
            self.guiUtility.scrollWindow = xrc.XRCCTRL(self.frame, "level0")
            self.guiUtility.mainSizer = self.guiUtility.scrollWindow.GetSizer()
            self.frame.topBackgroundRight = xrc.XRCCTRL(self.frame, "topBG3")
            self.guiUtility.scrollWindow.SetScrollbars(1,1,1024,768)
            self.guiUtility.scrollWindow.SetScrollRate(15,15)
            self.frame.mainButtonPersons = xrc.XRCCTRL(self.frame, "mainButtonPersons")

            self.frame.numberPersons = xrc.XRCCTRL(self.frame, "numberPersons")
            numperslabel = xrc.XRCCTRL(self.frame, "persons")
            self.frame.numberFiles = xrc.XRCCTRL(self.frame, "numberFiles")
            numfileslabel = xrc.XRCCTRL(self.frame, "files")
            self.frame.messageField = xrc.XRCCTRL(self.frame, "messageField")
            self.frame.firewallStatus = xrc.XRCCTRL(self.frame, "firewallStatus")
            tt = self.frame.firewallStatus.GetToolTip()
            if tt is not None:
                tt.SetTip(self.utility.lang.get('unknownreac_tooltip'))
            
            if sys.platform == "linux2":
                self.frame.numberPersons.SetFont(wx.Font(9,FONTFAMILY,FONTWEIGHT,wx.NORMAL,False,FONTFACE))
                self.frame.numberFiles.SetFont(wx.Font(9,FONTFAMILY,FONTWEIGHT,wx.NORMAL,False,FONTFACE))
                self.frame.messageField.SetFont(wx.Font(9,FONTFAMILY,FONTWEIGHT,wx.NORMAL,False,FONTFACE))
                numperslabel.SetFont(wx.Font(9,FONTFAMILY,FONTWEIGHT,wx.NORMAL,False,FONTFACE))
                numfileslabel.SetFont(wx.Font(9,FONTFAMILY,FONTWEIGHT,wx.NORMAL,False,FONTFACE))

            self.menubar = MainMenuBar(self.frame,self.utility)
            self.frame.set_wxapp(self)

            # Make sure self.utility.frame is set
            self.startAPI()
            
            #self.frame.Refresh()
            #self.frame.Layout()
            self.frame.Show(True)
            self.setDBStats()
            
            self.Bind(wx.EVT_QUERY_END_SESSION, self.frame.OnCloseWindow)
            self.Bind(wx.EVT_END_SESSION, self.frame.OnCloseWindow)
            
            # Arno, 2007-05-03: wxWidgets 2.8.3.0 and earlier have the MIME-type for .bmp 
            # files set to 'image/x-bmp' whereas 'image/bmp' is the official one.
            try:
                bmphand = None
                hands = wx.Image.GetHandlers()
                for hand in hands:
                    #print "Handler",hand.GetExtension(),hand.GetType(),hand.GetMimeType()
                    if hand.GetMimeType() == 'image/x-bmp':
                        bmphand = hand
                        break
                #wx.Image.AddHandler()
                if bmphand is not None:
                    bmphand.SetMimeType('image/bmp')
            except:
                # wx < 2.7 don't like wx.Image.GetHandlers()
                print_exc()
            
            # Must be after ABCLaunchMany is created
            self.torrentfeed = TorrentFeedThread.getInstance()
            self.torrentfeed.register(self.utility)
            self.torrentfeed.start()
            
            #print "DIM",wx.GetDisplaySize()
            #print "MM",wx.GetDisplaySizeMM()

            wx.CallAfter(self.startWithRightView)
            # Delay this so GUI has time to paint
            wx.CallAfter(self.loadSessionCheckpoint)
                        
            
        except Exception,e:
            print_exc()
            self.error = e
            self.onError()
            return False

        return True

    def startAPI(self):
        
        # Start Tribler Session
        state_dir = Session.get_default_state_dir()
        
        cfgfilename = Session.get_default_config_filename(state_dir)
        if DEBUG:
            print >>sys.stderr,"main: Session config",cfgfilename
        try:
            self.sconfig = SessionStartupConfig.load(cfgfilename)
        except:
            print_exc()
            self.sconfig = SessionStartupConfig()
            self.sconfig.set_state_dir(state_dir)
            # Set default Session params here
            torrcolldir = os.path.join(get_default_dest_dir(),STATEDIR_TORRENTCOLL_DIR)
            self.sconfig.set_torrent_collecting_dir(torrcolldir)
            self.sconfig.set_nat_detect(True)
            
            # rename old collected torrent directory
            try:
                old_collected_torrent_dir = os.path.join(state_dir, 'torrent2')
                if not os.path.exists(torrcolldir) and os.path.isdir(old_collected_torrent_dir):
                    os.rename(old_collected_torrent_dir, torrcolldir)
                    print >>sys.stderr,"main: Moved dir with old collected torrents to", torrcolldir
            except:
                print_exc()
        
        s = Session(self.sconfig)
        self.utility.session = s

        
        s.add_observer(self.sesscb_ntfy_reachable,NTFY_REACHABLE,[NTFY_INSERT])
        s.add_observer(self.sesscb_ntfy_activities,NTFY_ACTIVITIES,[NTFY_INSERT])
        s.add_observer(self.sesscb_ntfy_dbstats,NTFY_TORRENTS,[NTFY_INSERT])
        s.add_observer(self.sesscb_ntfy_dbstats,NTFY_PEERS,[NTFY_INSERT])

        # Load the default DownloadStartupConfig
        dlcfgfilename = get_default_dscfg_filename(s)
        try:
            defaultDLConfig = DefaultDownloadStartupConfig.load(dlcfgfilename)
        except:
            defaultDLConfig = DefaultDownloadStartupConfig.getInstance()
            #print_exc()
            defaultdestdir = os.path.join(get_default_dest_dir())
            defaultDLConfig.set_dest_dir(defaultdestdir)

        #print >>sys.stderr,"main: Read dlconfig",defaultDLConfig.dlconfig

        s.set_coopdlconfig(defaultDLConfig)

        # Loading of checkpointed Downloads delayed to allow GUI to paint,
        # see loadSessionCheckpoint

        # Create global rate limiter
        self.ratelimiter = UserDefinedMaxAlwaysOtherwiseEquallyDividedRateManager()
        self.rateadjustcount = 0 
        maxup = self.utility.config.Read('maxuploadrate', "int")
        maxdown = self.utility.config.Read('maxdownloadrate', "int")
        maxupseed = self.utility.config.Read('maxseeduploadrate', "int")
        self.ratelimiter.set_global_max_speed(UPLOAD,maxup)
        self.ratelimiter.set_global_max_speed(DOWNLOAD,maxdown)
        self.ratelimiter.set_global_max_seedupload_speed(maxupseed)
        self.utility.ratelimiter = self.ratelimiter
 
        # Only allow updates to come in after we defined ratelimiter
        s.set_download_states_callback(self.sesscb_states_callback)
        
    def sesscb_states_callback(self,dslist):
        """ Called by SessionThread """
        wx.CallAfter(self.gui_states_callback,dslist)
        return(1.0,False)
        
    def gui_states_callback(self,dslist):
        """ Called by MainThread  """
        print >>sys.stderr,"main: Stats:"
        #print >>sys.stderr,"main: Stats: NAT",self.utility.session.get_nat_type()
        try:
            # Pass DownloadStates to libaryView
            try:
                # Jelle: libraryMode only exists after user clicked button
                modedata = self.guiUtility.standardOverview.data['libraryMode']
                gm = modedata['grid'].gridManager
                gm.download_state_gui_callback(dslist)
            except KeyError:
                # Apparently libraryMode only has has a 'grid' key when visible
                pass
            except AttributeError:
                print_exc()
            except:
                print_exc()
            
            # Restart other torrents when the single torrent that was
            # running in VOD mode is done
            currdlist = []
            for ds in dslist:
                currdlist.append(ds.get_download())
            vodd = self.videoplayer.get_vod_download()
            for ds in dslist:
                d = ds.get_download()
                if d == vodd and ds.get_status() == DLSTATUS_SEEDING:
                    restartdlist = self.videoplayer.get_vod_postponed_downloads()
                    self.videoplayer.set_vod_postponed_downloads([]) # restart only once
                    for d in restartdlist:
                        if d in currdlist:
                            d.restart()
                    break
                            
            # Adjust speeds once every 4 seconds
            adjustspeeds = False
            if self.rateadjustcount % 4 == 0:
                adjustspeeds = True
            self.rateadjustcount += 1
    
            if adjustspeeds:
                self.ratelimiter.add_downloadstatelist(dslist)
                self.ratelimiter.adjust_speeds()
                
            # Update stats in lower right overview box
            self.guiUtility.refreshTorrentStats(dslist)
                
        except:
            print_exc()

    def loadSessionCheckpoint(self):
        # Load all other downloads
        # TODO: reset all saved DownloadConfig to new default?
        if self.params[0] != "":
            # There is something on the cmdline, start all stopped
            self.utility.session.load_checkpoint(initialdlstatus=DLSTATUS_STOPPED)
        else:
            self.utility.session.load_checkpoint()


    def sesscb_ntfy_dbstats(self,subject,changeType,objectID,*args):
        """ Called by SessionCallback thread """
        wx.CallAfter(self.setDBStats)
        
        
    def setDBStats(self):
        """ Set total # peers and torrents discovered """
        
        # Arno: GUI thread accessing database
        peer_db = self.utility.session.open_dbhandler(NTFY_PEERS)
        npeers = peer_db.getNumberPeers()
        torrent_db = self.utility.session.open_dbhandler(NTFY_TORRENTS)
        nfiles = torrent_db.getNumberTorrents()
        # Arno: not closing db connections, assuming main thread's will be 
        # closed at end.
                
        self.frame.numberPersons.SetLabel('%d' % npeers)
        self.frame.numberFiles.SetLabel('%d' % nfiles)
        

        
    def sesscb_ntfy_activities(self,subject,changeType,objectID,msg):
        # Called by SessionCallback thread
        #print >>sys.stderr,"main: sesscb_ntfy_activities called:",subject,changeType,objectID,msg
        wx.CallAfter(self.frame.setActivity,objectID,msg, self.utility)

    def sesscb_ntfy_reachable(self,subject,changeType,objectID,msg):
        wx.CallAfter(self.frame.onReachable)


    def videoserver_error_callback(self,e,url):
        """ Called by HTTP serving thread """
        wx.CallAfter(self.videoserver_error_guicallback,e,url)
        
    def videoserver_error_guicallback(self,e,url):
        print >>sys.stderr,"main: Video server reported error",str(e)
        #    self.show_error(str(e))
        pass

    def videoserver_set_status_callback(self,status):
        """ Called by HTTP serving thread """
        wx.CallAfter(self.videoserver_set_status_guicallback,status)

    def videoserver_set_status_guicallback(self,status):
        # TODO:
        if self.frame is not None:
            self.frame.set_player_status(status)


    def onError(self,source=None):
        # Don't use language independence stuff, self.utility may not be
        # valid.
        msg = "Unfortunately, Tribler ran into an internal error:\n\n"
        if source is not None:
            msg += source
        msg += str(self.error.__class__)+':'+str(self.error)
        msg += '\n'
        msg += 'Please see the FAQ on www.tribler.org on how to act.'
        dlg = wx.MessageDialog(None, msg, "Tribler Fatal Error", wx.OK|wx.ICON_ERROR)
        result = dlg.ShowModal()
        print_exc()
        dlg.Destroy()

    def MacOpenFile(self,filename):
        self.utility.queue.addtorrents.AddTorrentFromFile(filename)

    def OnExit(self):
        print >>sys.stderr,"main: ONEXIT"
        
        self.torrentfeed.shutdown()

        # Don't checkpoint, interferes with current way of saving Preferences,
        # see Tribler/Main/Dialogs/abcoption.py
        self.utility.session.shutdown(hacksessconfcheckpoint=False) 
        
        if not ALLOW_MULTIPLE:
            del self.single_instance_checker
        return 0
    
    def db_exception_handler(self,e):
        if DEBUG:
            print >> sys.stderr,"abc: Database Exception handler called",e,"value",e.args,"#"
        try:
            if e.args[1] == "DB object has been closed":
                return # We caused this non-fatal error, don't show.
            if self.error is not None and self.error.args[1] == e.args[1]:
                return # don't repeat same error
        except:
            print >> sys.stderr, "abc: db_exception_handler error", e, type(e)
            print_exc()
            #print_stack()
        self.error = e
        onerror_lambda = lambda:self.onError(source="The database layer reported:  ") 
        wx.CallAfter(onerror_lambda)
    
    def getConfigPath(self):
        return self.utility.getConfigPath()

    def startWithRightView(self):
        if self.params[0] != "":
            self.guiUtility.standardLibraryOverview()
 
 
    def i2icallback(self,cmd,param):
        """ Called by Instance2Instance thread """
        
        print >>sys.stderr,"abc: Another instance called us with cmd",cmd,"param",param
        
        if cmd == 'START':
            torrentfilename = None
            if param.startswith('http:'):
                # Retrieve from web 
                f = tempfile.NamedTemporaryFile()
                n = urllib2.urlopen(url)
                data = n.read()
                f.write(data)
                f.close()
                n.close()
                torrentfilename = f.name
            else:
                torrentfilename = param
                
            # Switch to GUI thread
            start_download_lambda = lambda:self.frame.startDownload(torrentfilename)
            wx.CallAfter(start_download_lambda)
    
        
class DummySingleInstanceChecker:
    
    def __init__(self,basename):
        pass

    def IsAnotherRunning(self):
        "Uses pgrep to find other tribler.py processes"
        # If no pgrep available, it will always start tribler
        progressInfo = commands.getoutput('pgrep -fl "tribler\.py" | grep -v pgrep')
        numProcesses = len(progressInfo.split('\n'))
        if DEBUG:
            print 'ProgressInfo: %s, num: %d' % (progressInfo, numProcesses)
        return numProcesses > 1
                
        
##############################################################
#
# Main Program Start Here
#
##############################################################
def run(params = None):
    if params is None:
        params = [""]
    
    if len(sys.argv) > 1:
        params = sys.argv[1:]
    
    # Create single instance semaphore
    # Arno: On Linux and wxPython-2.8.1.1 the SingleInstanceChecker appears
    # to mess up stderr, i.e., I get IOErrors when writing to it via print_exc()
    #
    if sys.platform != 'linux2':
        single_instance_checker = wx.SingleInstanceChecker("tribler-" + wx.GetUserId())
    else:
        single_instance_checker = DummySingleInstanceChecker("tribler-")

    if not ALLOW_MULTIPLE and single_instance_checker.IsAnotherRunning():
        #Send  torrent info to abc single instance
        if params[0] != "":
            torrentfilename = params[0]
            i2ic = Instance2InstanceClient(I2I_LISTENPORT,'START',torrentfilename)
    else:
        arg0 = sys.argv[0].lower()
        if arg0.endswith('.exe'):
            installdir = os.path.abspath(os.path.dirname(sys.argv[0]))
        else:
            installdir = os.getcwd()  
        # Arno: don't chdir to allow testing as other user from other dir.
        #os.chdir(installdir)

        # Launch first abc single instance
        app = ABCApp(0, params, single_instance_checker, installdir)
        configpath = app.getConfigPath()
        app.MainLoop()

    print "Client shutting down. Sleeping for a few seconds to allow other threads to finish"
    sleep(1)

    # This is the right place to close the database, unfortunately Linux has
    # a problem, see ABCFrame.OnCloseWindow
    #
    #if sys.platform != 'linux2':
    #    tribler_done(configpath)
    #os._exit(0)

if __name__ == '__main__':
    run()

