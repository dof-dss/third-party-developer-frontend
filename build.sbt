import com.typesafe.sbt.digest.Import._
import com.typesafe.sbt.uglify.Import._
import com.typesafe.sbt.web.Import._
import net.ground5hark.sbt.concat.Import._
import play.core.PlayVersion
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.PublishingSettings._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

import scala.util.Properties

lazy val appName = "third-party-developer-frontend"

lazy val appDependencies: Seq[ModuleID] = compile ++ test

lazy val t2vVersion = "0.14.2"
lazy val cucumberVersion = "1.2.5"
lazy val seleniumVersion = "2.53.1"
lazy val enumeratumVersion = "1.5.13"

val bootStrapPlayVersion = "0.32.0"

lazy val compile = Seq(
  "uk.gov.hmrc" %% "bootstrap-play-26" % bootStrapPlayVersion,
  "uk.gov.hmrc" %% "govuk-template" % "5.26.0-play-26",
  "uk.gov.hmrc" %% "play-ui" % "7.27.0-play-26",
  "uk.gov.hmrc" %% "url-builder" % "3.0.0",
  "uk.gov.hmrc" %% "play-json-union-formatter" % "1.4.0",
  "uk.gov.hmrc" %% "http-metrics" % "1.3.0",
  "uk.gov.hmrc" %% "json-encryption" % "4.1.0",
  "uk.gov.hmrc" %% "emailaddress" % "2.2.0",
  "uk.gov.hmrc" %% "play-conditional-form-mapping" % "0.2.0",
  "com.typesafe.play" %% "play-json-joda" % "2.6.10",
  "io.dropwizard.metrics" % "metrics-graphite" % "3.2.6",
  "com.beachape" %% "enumeratum" % enumeratumVersion,
  "com.beachape" %% "enumeratum-play" % enumeratumVersion,
  "com.google.zxing" % "core" % "3.3.3",
  "de.threedimensions" %% "metrics-play" % "2.5.13",
  "jp.t2v" %% "play2-auth" % t2vVersion
)

lazy val test = Seq(
  "info.cukes" %% "cucumber-scala" % cucumberVersion % "test",
  "info.cukes" % "cucumber-junit" % cucumberVersion % "test",
  "uk.gov.hmrc" %% "bootstrap-play-26" % bootStrapPlayVersion % Test classifier "tests",
  "uk.gov.hmrc" %% "hmrctest" % "3.3.0" % "test",
  "junit" % "junit" % "4.12" % "test",
  "org.jsoup" % "jsoup" % "1.11.3" % "test",
  "org.pegdown" % "pegdown" % "1.6.0" % "test",
  "com.typesafe.play" %% "play-test" % PlayVersion.current % "test",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % "test",
  "org.seleniumhq.selenium" % "selenium-java" % seleniumVersion % "test",
  "com.github.tomakehurst" % "wiremock" % "1.58" % "test",
  "org.mockito" % "mockito-all" % "1.10.19" % "test",
  "jp.t2v" %% "play2-auth-test" % t2vVersion % "test",
  "org.scalaj" %% "scalaj-http" % "2.3.0" % "test",
  "org.scalacheck" %% "scalacheck" % "1.14.0" % "test",
  "com.github.mkolisnyk" % "cucumber-reports" % "1.0.7" % "test",
  "net.masterthought" % "cucumber-reporting" % "3.20.0" % "test",
  "net.masterthought" % "cucumber-sandwich" % "3.20.0" % "test",
  "com.assertthat" % "selenium-shutterbug" % "0.9" % "test"
)
lazy val overrideDependencies = Set(
  "org.seleniumhq.selenium" % "selenium-java" % seleniumVersion % "test",
  "org.seleniumhq.selenium" % "selenium-htmlunit-driver" % seleniumVersion % "test"
)

lazy val plugins: Seq[Plugins] = Seq(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
lazy val playSettings: Seq[Setting[_]] = Seq.empty
lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins: _*)
  .settings(
    Concat.groups := Seq(
      "javascripts/apis-app.js" -> group(
        (baseDirectory.value / "app" / "assets" / "javascripts") ** "*.js"
      )
    ),
    uglifyCompressOptions := Seq(
      "unused=true",
      "dead_code=true"
    ),
    includeFilter in uglify := GlobFilter("apis-*.js"),
    pipelineStages := Seq(digest),
    pipelineStages in Assets := Seq(
      concat,
      uglify
    )
  )
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    targetJvm := "jvm-1.8",
    libraryDependencies ++= appDependencies,
    dependencyOverrides ++= overrideDependencies,
    parallelExecution in Test := false,
    fork in Test := false,
    retrieveManaged := true,
    scalaVersion := "2.11.11",
    resolvers += Resolver.jcenterRepo
  )
  .settings(playPublishingSettings: _*)
  .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
  .settings(testOptions in Test := Seq(Tests.Filter(unitFilter), Tests.Argument(TestFrameworks.ScalaTest, "-eT"))
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    testOptions in IntegrationTest := Seq(Tests.Filter(integrationTestFilter), Tests.Argument(TestFrameworks.ScalaTest, "-eT")),
    unmanagedSourceDirectories in IntegrationTest <<= (baseDirectory in IntegrationTest) (base => Seq(base / "test")),
    unmanagedResourceDirectories in IntegrationTest <<= (baseDirectory in IntegrationTest) (base => Seq(base / "test")),
    // unmanagedResourceDirectories in IntegrationTest <+= baseDirectory(_ / "target/web/public/test"),
    // testOptions in IntegrationTest += Tests.Setup(() => System.setProperty("javascript.enabled", "true")),
    //testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    parallelExecution in IntegrationTest := false
  )
  .configs(ComponentTest)
  .settings(inConfig(ComponentTest)(Defaults.testSettings): _*)
  .settings(
    testOptions in ComponentTest := Seq(Tests.Filter(componentTestFilter), Tests.Argument(TestFrameworks.ScalaTest, "-eT")),
    unmanagedSourceDirectories in ComponentTest <<= (baseDirectory in ComponentTest) (base => Seq(base / "test")),
    unmanagedResourceDirectories in ComponentTest <<= (baseDirectory in ComponentTest) (base => Seq(base / "test")),
    unmanagedResourceDirectories in ComponentTest <+= baseDirectory(_ / "target/web/public/test"),
    testOptions in ComponentTest += Tests.Setup(() => System.setProperty("javascript.enabled", "true")),
    testGrouping in ComponentTest := oneForkedJvmPerTest((definedTests in ComponentTest).value),
    parallelExecution in ComponentTest := false
  )
  .settings(majorVersion := 0)
lazy val allPhases = "tt->test;test->test;test->compile;compile->compile"
lazy val IntegrationTest = config("it") extend Test
lazy val ComponentTest = config("component") extend Test
lazy val TemplateTest = config("tt") extend Test
lazy val playPublishingSettings: Seq[sbt.Setting[_]] = Seq(

  credentials += SbtCredentials,

  publishArtifact in(Compile, packageDoc) := false,
  publishArtifact in(Compile, packageSrc) := false
) ++
  publishAllArtefacts

def unitFilter(name: String): Boolean = name startsWith "unit"
def integrationTestFilter(name: String): Boolean = name startsWith "it"
def componentTestFilter(name: String): Boolean = name startsWith "component.js"

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
  tests map { test =>
    Group(
      test.name,
      Seq(test),
      SubProcess(
        ForkOptions(
          runJVMOptions = Seq(
            "-Dtest.name=" + test.name,
            s"-Dtest_driver=${Properties.propOrElse("test_driver", "chrome")}"
          )
        )
      )
    )
  }

// Coverage configuration
coverageMinimum := 85
coverageFailOnMinimum := true
coverageExcludedPackages := "<empty>;com.kenshoo.play.metrics.*;.*definition.*;prod.*;app.*;uk.gov.hmrc.BuildInfo"