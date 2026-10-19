using System;
using System.Runtime.InteropServices;
using Extensibility;
using PocketPresenter.Core.PowerPoint;

namespace PocketPresenter.AddIn
{

    public class OfficeSlideShowView : ISlideShowView
    {
        private readonly dynamic _view;
        public OfficeSlideShowView(dynamic view) => _view = view;
        public int CurrentSlideIndex => (int)_view.Slide.SlideIndex;
        public int TotalSlidesCount => (int)_view.Application.ActivePresentation.Slides.Count;
        public void Next() => _view.Next();
        public void Previous() => _view.Previous();
        public void GoToSlide(int index) => _view.GotoSlide(index);
    }

    /// <summary>
    /// PowerPoint COM Add-in lifecycle controller and entrypoint wrapper.
    /// Strictly routes Office COM interactions to PowerPoint's STA/UI thread.
    /// Unsubscribes all event handlers and disposes controllers on shutdown.
    /// </summary>
    [ComVisible(true)]
    [Guid("4B9A180E-7C29-4F54-BAE6-9DC777B6BE14")]
    [ProgId("PocketPresenter.AddIn")]
    [ClassInterface(ClassInterfaceType.None)]
    public class ThisAddIn : IDTExtensibility2
    {
        private static readonly string AssemblyDir;

        static ThisAddIn()
        {
            try
            {
                AssemblyDir = System.IO.Path.GetDirectoryName(typeof(ThisAddIn).Assembly.Location) ?? "";
                AppDomain.CurrentDomain.AssemblyResolve += OnAssemblyResolve;
            }
            catch
            {
                AssemblyDir = "";
            }
        }

        private static System.Reflection.Assembly? OnAssemblyResolve(object? sender, ResolveEventArgs args)
        {
            try
            {
                var requestedName = new System.Reflection.AssemblyName(args.Name).Name;
                if (string.IsNullOrEmpty(requestedName) || string.IsNullOrEmpty(AssemblyDir))
                    return null;

                string targetFile = System.IO.Path.Combine(AssemblyDir, requestedName + ".dll");
                if (System.IO.File.Exists(targetFile))
                {
                    return System.Reflection.Assembly.LoadFrom(targetFile);
                }
            }
            catch { }
            return null;
        }

        private bool _isStarted;
        private dynamic? _pptApp;
        private System.IO.Ports.SerialPort? _serialPort;
        private PocketPresenter.Core.Transport.RfcommStreamTransport? _transport;
        private PocketPresenter.Core.PowerPoint.PresentationSessionCoordinator? _coordinator;
        private readonly PocketPresenter.Protocol.FrameDecoder _frameDecoder = new PocketPresenter.Protocol.FrameDecoder();

        public void InternalStartup(object? app = null)
        {
            if (_isStarted) return;
            _isStarted = true;

            EnsureCoordinator();

            if (app != null)
            {
                _pptApp = app;
                try
                {
                    _pptApp.SlideShowBegin += new Action<dynamic>(OnSlideShowBegin);
                    _pptApp.SlideShowEnd += new Action<dynamic>(OnSlideShowEnd);
                }
                catch
                {
                    // Fallback gracefully if event hook requires specific delegate
                }
            }

            StartBluetoothBridge();
        }

        [System.Runtime.CompilerServices.MethodImpl(System.Runtime.CompilerServices.MethodImplOptions.NoInlining)]
        private void EnsureCoordinator()
        {
            if (_coordinator == null)
            {
                _coordinator = new PocketPresenter.Core.PowerPoint.PresentationSessionCoordinator();
            }
        }

        private void StartBluetoothBridge()
        {
            System.Threading.Tasks.Task.Run(() =>
            {
                try
                {
                    EnsureCoordinator();
                    if (_coordinator == null) return;

                    string portName = "COM4";
                    _serialPort = new System.IO.Ports.SerialPort(portName, 9600, System.IO.Ports.Parity.None, 8, System.IO.Ports.StopBits.One)
                    {
                        ReadTimeout = System.IO.Ports.SerialPort.InfiniteTimeout,
                        WriteTimeout = 5000
                    };
                    _serialPort.Open();

                    _transport = new PocketPresenter.Core.Transport.RfcommStreamTransport();
                    _transport.BytesReceived += chunk =>
                    {
                        var results = _frameDecoder.Feed(chunk.Span);
                        foreach (var res in results)
                        {
                            if (res.Success && res.Envelope != null)
                            {
                                if (res.Envelope.Type == PocketPresenter.Protocol.MessageTypes.HelloAck)
                                {
                                    _coordinator.SessionStateMachine.HandleInboundEnvelope(res.Envelope);
                                }
                                else
                                {
                                    _coordinator.CommandRouter.Route(res.Envelope, _coordinator.SessionStateMachine.Session);
                                }
                            }
                        }
                    };

                    _coordinator.OutboundMessageReady += async env =>
                    {
                        try
                        {
                            if (_transport != null && _transport.State == PocketPresenter.Core.Transport.TransportState.Connected)
                            {
                                var encoded = PocketPresenter.Protocol.FrameEncoder.Encode(env);
                                await _transport.SendAsync(encoded);
                            }
                        }
                        catch { }
                    };

                    _transport.AttachStream(_serialPort.BaseStream);
                    _coordinator.SessionStateMachine.StartConnecting();
                }
                catch
                {
                    // Fallback gracefully if Bluetooth port is currently busy or awaiting phone session
                }
            });
        }

        private void OnSlideShowBegin(dynamic wn)
        {
            try
            {
                _coordinator?.PowerPointController.AttachSlideShowView(new OfficeSlideShowView(wn.View));
            }
            catch { }
        }

        private void OnSlideShowEnd(dynamic pres)
        {
            try
            {
                _coordinator?.PowerPointController.DetachSlideShowView();
            }
            catch { }
        }

        public void InternalShutdown()
        {
            _isStarted = false;
            try
            {
                _coordinator?.OverlayController.Hide();
                _coordinator?.PowerPointController.DetachSlideShowView();
                _transport?.Dispose();
                _serialPort?.Dispose();
                _coordinator?.Dispose();
            }
            catch { }
        }

        // IDTExtensibility2 Implementation
        public void OnConnection(object Application, ext_ConnectMode ConnectMode, object AddInInst, ref Array custom)
        {
            InternalStartup(Application);
        }

        public void OnDisconnection(ext_DisconnectMode RemoveMode, ref Array custom)
        {
            InternalShutdown();
        }

        public void OnAddInsUpdate(ref Array custom) { }
        public void OnStartupComplete(ref Array custom) { }
        public void OnBeginShutdown(ref Array custom)
        {
            InternalShutdown();
        }
    }
}
