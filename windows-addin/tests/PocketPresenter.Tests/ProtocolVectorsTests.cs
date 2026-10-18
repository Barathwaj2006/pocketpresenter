using System;
using System.Collections.Generic;
using System.IO;
using System.Text.Json;
using PocketPresenter.Protocol;
using Xunit;

namespace PocketPresenter.Tests
{
    public class ProtocolVectorsTests
    {
        private static string FindVectorsJsonPath()
        {
            var dir = AppDomain.CurrentDomain.BaseDirectory;
            while (!string.IsNullOrEmpty(dir))
            {
                var candidate = Path.Combine(dir, "protocol", "fixtures", "vectors.json");
                if (File.Exists(candidate))
                {
                    return candidate;
                }
                var parent = Directory.GetParent(dir);
                if (parent == null || parent.FullName == dir) break;
                dir = parent.FullName;
            }

            throw new FileNotFoundException("Could not locate protocol/fixtures/vectors.json in any parent directory.");
        }

        [Fact]
        public void AllVectorsInJson_PassValidation()
        {
            var filePath = FindVectorsJsonPath();
            Assert.True(File.Exists(filePath), $"vectors.json not found at {filePath}");

            var jsonContent = File.ReadAllText(filePath);
            using var doc = JsonDocument.Parse(jsonContent);
            var root = doc.RootElement;

            var defaultSid = root.GetProperty("session").GetProperty("sid").GetString()!;
            var vectors = root.GetProperty("vectors").EnumerateArray();

            foreach (var vector in vectors)
            {
                var id = vector.GetProperty("id").GetString()!;
                var handshakeComplete = vector.GetProperty("handshakeComplete").GetBoolean();
                var expectedList = vector.GetProperty("expect").EnumerateArray();

                var session = new ProtocolSession
                {
                    HandshakeComplete = handshakeComplete,
                    SessionId = handshakeComplete ? defaultSid : null
                };

                var decoder = new FrameDecoder();
                var actualResults = new List<(bool isOk, string? type, int seq, string? errorCode, bool closeConnection)>();

                if (vector.TryGetProperty("reads", out var readsProp))
                {
                    foreach (var readElement in readsProp.EnumerateArray())
                    {
                        var chunk = readElement.GetString()!;
                        var decodeResults = decoder.Feed(chunk);

                        foreach (var dr in decodeResults)
                        {
                            if (!dr.Success)
                            {
                                actualResults.Add((false, null, 0, dr.ErrorCode, dr.CloseConnection));
                            }
                            else
                            {
                                var vr = ProtocolValidator.Validate(dr.Envelope!, session);
                                if (vr.IsOk)
                                {
                                    session.MarkSequenceExecuted(dr.Envelope!.Sequence);
                                    actualResults.Add((true, dr.Envelope!.Type, dr.Envelope!.Sequence, null, false));
                                }
                                else
                                {
                                    actualResults.Add((false, dr.Envelope!.Type, dr.Envelope!.Sequence, vr.ErrorCode, vr.CloseConnection));
                                }
                            }
                        }
                    }
                }
                else if (vector.TryGetProperty("generate", out var genProp))
                {
                    var padToBytes = genProp.GetProperty("padToBytes").GetInt32();
                    var padContent = new string('x', Math.Max(0, padToBytes - 120));
                    var largeJson = $"{{\"v\":1,\"sid\":\"{defaultSid}\",\"seq\":1,\"type\":\"slide.next\",\"payload\":{{\"pad\":\"{padContent}\"}}}}\n";
                    if (largeJson.Length < padToBytes)
                    {
                        largeJson = largeJson.PadRight(padToBytes - 1, ' ') + "\n";
                    }

                    var decodeResults = decoder.Feed(largeJson);
                    foreach (var dr in decodeResults)
                    {
                        if (!dr.Success)
                        {
                            actualResults.Add((false, null, 0, dr.ErrorCode, dr.CloseConnection));
                        }
                        else
                        {
                            var vr = ProtocolValidator.Validate(dr.Envelope!, session);
                            if (vr.IsOk)
                            {
                                actualResults.Add((true, dr.Envelope!.Type, dr.Envelope!.Sequence, null, false));
                            }
                            else
                            {
                                actualResults.Add((false, dr.Envelope!.Type, dr.Envelope!.Sequence, vr.ErrorCode, vr.CloseConnection));
                            }
                        }
                    }
                }

                // Verify expectations
                int idx = 0;
                foreach (var exp in expectedList)
                {
                    Assert.True(idx < actualResults.Count, $"Vector {id}: Expected item at index {idx}, but only {actualResults.Count} results produced.");
                    var actual = actualResults[idx];
                    var expectedResult = exp.GetProperty("result").GetString()!;

                    if (expectedResult == "ok")
                    {
                        Assert.True(actual.isOk, $"Vector {id} [{idx}]: Expected 'ok', but got error '{actual.errorCode}'");
                        if (exp.TryGetProperty("type", out var typeProp))
                        {
                            Assert.Equal(typeProp.GetString(), actual.type);
                        }
                        if (exp.TryGetProperty("seq", out var seqProp))
                        {
                            Assert.Equal(seqProp.GetInt32(), actual.seq);
                        }
                    }
                    else
                    {
                        Assert.False(actual.isOk, $"Vector {id} [{idx}]: Expected error, but got ok.");
                        if (exp.TryGetProperty("code", out var codeProp))
                        {
                            Assert.Equal(codeProp.GetString(), actual.errorCode);
                        }
                        if (exp.TryGetProperty("closeConnection", out var closeProp))
                        {
                            Assert.Equal(closeProp.GetBoolean(), actual.closeConnection);
                        }
                    }

                    idx++;
                }

                Assert.Equal(idx, actualResults.Count);
            }
        }
    }
}
