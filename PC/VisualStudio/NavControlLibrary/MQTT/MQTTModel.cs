using MQTTnet;
using MQTTnet.Client.Connecting;
using MQTTnet.Client.Disconnecting;
using MQTTnet.Client.Options;
using MQTTnet.Client.Receiving;
using MQTTnet.Extensions.ManagedClient;
using MQTTnet.Protocol;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;

namespace NavControlLibrary.MQTT
{
    public class MQTTModel : NotifyModel
    {
        private IManagedMqttClient managedMqttClient = null;

        protected string mServer = "";
        protected int mPort = 1883;
        protected string mLogin = "";
        protected string mPassword = "";
        protected string mPath = "";
        protected int mTID = 6080;
        protected bool mSSL = false;

        bool mOnline = false;

        public TraceLog mLog = new TraceLog();
        public TraceLog mMQTTLog = new TraceLog();
        public TraceLog mInformerLog = new TraceLog();

        #region События
        public delegate void eReceive(object sender, string topic, string msg, DateTime time);
        public event eReceive onReceive;
        #endregion События


        #region Свойства
        [JsonProperty("ssl")]
        public bool SSL
        {
            get { return mSSL; }
            set
            {
                mSSL = value;
                NotifyPropertyChanged("SSL");
            }
        }
        [JsonProperty("tid")]
        public int TID
        {
            get { return mTID; }
            set
            {
                mTID = value;
                mPath = "navigator/" + TID.ToString();
                NotifyPropertyChanged("TID");
            }
        }

        [JsonIgnore]
        public bool IsRun
        {
            get { return managedMqttClient != null; }
        }

        [JsonProperty("server")]
        public string Server
        {
            get { return mServer; }
            set
            {
                mServer = value;
                NotifyPropertyChanged("Server");
            }
        }
        [JsonProperty("port")]
        public int Port
        {
            get { return mPort; }
            set
            {
                mPort = value;
                NotifyPropertyChanged("Port");
            }
        }
        [JsonProperty("login")]
        public string Login
        {
            get { return mLogin; }
            set
            {
                mLogin = value;
                NotifyPropertyChanged("Login");
            }
        }
        [JsonProperty("password")]
        public string Password
        {
            get { return mPassword; }
            set
            {
                mPassword = value;
                NotifyPropertyChanged("Password");
            }
        }

        [JsonIgnore]
        public bool Online
        {
            get
            {
                return mOnline;
            }
            set
            {
                mOnline = value;
                NotifyPropertyChanged("Online");
            }
        }
        #endregion Свойства

        public async void Connect()
        {
            if (!IsRun)
            {
                var mqttFactory = new MqttFactory();

                MqttClientOptionsBuilder opt = new MqttClientOptionsBuilder()
                       .WithClientId("MQTTLogReader")
                       .WithTcpServer(Server, Port)
                       .WithProtocolVersion(MQTTnet.Formatter.MqttProtocolVersion.V500)
                       .WithCredentials(Login, Password);
                if (SSL)
                {
                    opt.WithTls(new MqttClientOptionsBuilderTlsParameters
                    {
                        UseTls = true,
                        AllowUntrustedCertificates = true,
                        SslProtocol = System.Security.Authentication.SslProtocols.Tls12,
                        //IgnoreCertificateChainErrors = true,
                        //IgnoreCertificateRevocationErrors = true
                    });
                }

                ManagedMqttClientOptions options = new ManagedMqttClientOptionsBuilder()
                   .WithAutoReconnectDelay(TimeSpan.FromSeconds(5))
                   .WithClientOptions(opt.Build())
                   .Build();

                this.managedMqttClient = mqttFactory.CreateManagedMqttClient();
                //this.managedMqttClientPublisher.UseApplicationMessageReceivedHandler(this.HandleReceivedApplicationMessage);
                this.managedMqttClient.ConnectedHandler = new MqttClientConnectedHandlerDelegate(OnPublisherConnected);
                this.managedMqttClient.DisconnectedHandler = new MqttClientDisconnectedHandlerDelegate(OnPublisherDisconnected);
                this.managedMqttClient.ApplicationMessageReceivedHandler = new MqttApplicationMessageReceivedHandlerDelegate(OnSubscriberMessageReceived);
                await this.managedMqttClient.StartAsync(options);
                NotifyPropertyChanged("IsRun");
            }
        }

        public async void Disconnect()
        {
            if (IsRun)
            {
                await this.managedMqttClient.StopAsync();

                managedMqttClient = null;
                NotifyPropertyChanged("IsRun");
            }
        }

        private void OnPublisherDisconnected(MqttClientDisconnectedEventArgs obj)
        {
            if (obj.Exception == null) AddLog(mLog, "Disconnected");
            else AddLog(mLog, "Disconnected: " + obj.Exception.Message);
            Online = false;
        }

        private async void OnPublisherConnected(MqttClientConnectedEventArgs obj)
        {
            AddLog(mLog, "Connected");

            if (this.managedMqttClient != null)
            {
                await this.managedMqttClient.SubscribeAsync(new MqttTopicFilterBuilder().WithTopic(mPath + "/#").Build());
                AddLog(mLog, "Subscribe on " + mPath + "/#");
            }
        }

        private void OnSubscriberMessageReceived(MqttApplicationMessageReceivedEventArgs obj)
        {
            string topic = obj.ApplicationMessage.Topic.Substring(mPath.Length + 1);
            string msg = obj.ApplicationMessage.ConvertPayloadToString();

            if (topic == "status")
            {
                if (msg == null) Online = false;
                else
                {
                    JObject status = JObject.Parse(msg);
                    if ((status["client"] != null) && ((string)status["client"] == "online")) Online = true;
                    else Online = false;
                }
            }


            if (onReceive != null) onReceive(this, topic, msg, DateTime.Now);
            AddLog(mLog, topic + ":" + msg, TraceLog.TraceLogItem.LogType.RX);
            if ((topic == "MQTT") || (topic == "GPS")) AddLog(mMQTTLog, msg);
            if (topic == "Informer") AddLog(mInformerLog, msg);
        }

        public async void Send(string topic, string msg)
        {
            if (IsRun && managedMqttClient.IsConnected)
            {
                try
                {
                    JObject status = JObject.Parse(msg);

                    var message = new MqttApplicationMessageBuilder().WithTopic(mPath + "/" + topic).WithPayload(msg).WithQualityOfServiceLevel(MqttQualityOfServiceLevel.AtLeastOnce).Build();
                    await managedMqttClient.PublishAsync(message);
                    AddLog(mLog, topic + ":" + msg, TraceLog.TraceLogItem.LogType.TX);
                }
                catch (Exception e)
                {
                    AddLog(mLog, topic + ":" + msg, TraceLog.TraceLogItem.LogType.ERROR);
                }
            }
        }

        private delegate void pAddLog(TraceLog lg, string text, TraceLog.TraceLogItem.LogType tp);
        private void AddLog(TraceLog lg, string text, TraceLog.TraceLogItem.LogType tp = TraceLog.TraceLogItem.LogType.INFO)
        {
            if (mLog != null)
            {
                if (!System.Windows.Application.Current.Dispatcher.CheckAccess())
                {
                    pAddLog d = new pAddLog(AddLog);
                    System.Windows.Application.Current.Dispatcher.Invoke(d, new object[] { lg, text, tp });
                }
                else
                {
                    lg.AddTrace(text, tp);
                }
            }
        }
    }
}
