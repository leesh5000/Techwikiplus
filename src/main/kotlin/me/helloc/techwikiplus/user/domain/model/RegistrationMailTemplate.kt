package me.helloc.techwikiplus.user.domain.model

class RegistrationMailTemplate {
    companion object {
        private const val CODE_PLACEHOLDER = "{{code}}"

        fun of(registrationCode: RegistrationCode): MailContent {
            return MailContent(
                subject = getSubject(),
                body = getHtmlTemplate(registrationCode).replace(CODE_PLACEHOLDER, registrationCode.value),
            )
        }

        private fun getSubject(): String = "TechWiki+ 회원가입 인증 코드"

        private fun getHtmlTemplate(registrationCode: RegistrationCode): String {
            return TEMPLATE.replace(CODE_PLACEHOLDER, registrationCode.value)
        }

        private const val TEMPLATE = """
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>TechWiki+ 이메일 인증</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f3f4f6; line-height: 1.6;">
                <table cellpadding="0" cellspacing="0" border="0" width="100%" style="min-width: 100%; background-color: #f3f4f6;">
                    <tr>
                        <td align="center" style="padding: 40px 20px;">
                            <table cellpadding="0" cellspacing="0" border="0" style="max-width: 100%; width: 600px; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);">
                                <!-- Header -->
                                <tr>
                                    <td style="background: linear-gradient(135deg, #2563eb 0%, #1e40af 100%); padding: 40px 40px 30px; text-align: center; border-radius: 8px 8px 0 0;">
                                        <h1 style="margin: 0; color: #ffffff; font-size: 28px; font-weight: 700; letter-spacing: -0.5px;">TechWiki+</h1>
                                        <p style="margin: 10px 0 0; color: #e0e7ff; font-size: 16px; font-weight: 400;">이메일 인증 코드</p>
                                    </td>
                                </tr>
                                <!-- Content -->
                                <tr>
                                    <td style="padding: 40px;">
                                        <h2 style="margin: 0 0 20px; color: #111827; font-size: 24px; font-weight: 600; text-align: center;">이메일 주소를 인증해주세요</h2>
                                        <p style="margin: 0 0 30px; color: #6b7280; font-size: 16px; text-align: center; line-height: 1.6;">
                                            안녕하세요!<br>
                                            TechWiki+ 회원가입을 위해 아래 인증 코드를 입력해주세요.
                                        </p>
                                        <!-- Verification Code -->
                                        <div style="background-color: #f3f4f6; border-radius: 8px; padding: 30px; text-align: center; margin: 0 0 30px;">
                                            <p style="margin: 0 0 10px; color: #6b7280; font-size: 14px; text-transform: uppercase; letter-spacing: 1px;">인증 코드</p>
                                            <div style="font-size: 36px; font-weight: 700; color: #2563eb; letter-spacing: 8px; font-family: 'Courier New', monospace;">{{code}}</div>
                                        </div>
                                        <div style="background-color: #fef3c7; border-left: 4px solid #f59e0b; padding: 16px; margin: 0 0 30px; border-radius: 4px;">
                                            <p style="margin: 0; color: #92400e; font-size: 14px; line-height: 1.5;">
                                                <strong>중요:</strong> 이 코드는 10분간 유효합니다. 보안을 위해 타인과 공유하지 마세요.
                                            </p>
                                        </div>
                                        <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 30px 0;">
                                        <p style="margin: 0 0 20px; color: #9ca3af; font-size: 14px; text-align: center;">
                                            이 메일을 요청하지 않은 경우 무시하셔도 됩니다.
                                        </p>
                                    </td>
                                </tr>
                                <!-- Footer -->
                                <tr>
                                    <td style="background-color: #f9fafb; padding: 30px 40px; text-align: center; border-radius: 0 0 8px 8px;">
                                        <p style="margin: 0 0 10px; color: #6b7280; font-size: 14px;">
                                            도움이 필요하신가요? <a href="mailto:support@techwikiplus.com" style="color: #2563eb; text-decoration: none; font-weight: 600;">고객 지원</a>
                                        </p>
                                        <p style="margin: 0; color: #9ca3af; font-size: 12px;">
                                            © 2025 TechWiki+. All rights reserved.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
        </html>
    """
    }
}
